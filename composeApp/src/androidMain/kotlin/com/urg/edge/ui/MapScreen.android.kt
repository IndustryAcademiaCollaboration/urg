package com.urg.edge.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.urg.edge.routing.BRouterEngine
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.urg.edge.map.MapDownloadManager
import com.urg.edge.map.Rd5DownloadManager
import com.urg.edge.map.getPrefectureFileName
import com.urg.edge.shelter.Shelter
import com.urg.edge.shelter.ShelterRepository
import com.urg.edge.shelter.ShelterType
import com.urg.edge.TriageResult
import com.urg.edge.VictimRecord
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

// 施設フィルター選択肢
enum class FacilityFilter(val displayName: String) {
    ALL("全て"),
    EVACUATION_CENTER("指定避難所"),
    EMERGENCY_SHELTER("指定緊急避難場所"),
    FIRST_AID_STATION("救護所"),
    HOSPITAL("病院")
}

// GeoJSONソースID・レイヤーID定数
private const val SOURCE_SHELTERS = "source-shelters"
private const val SOURCE_CURRENT_LOCATION = "source-current-location"
private const val LAYER_SHELTERS = "layer-shelters"
private const val LAYER_SHELTER_LABELS = "layer-shelter-labels"
private const val LAYER_CURRENT_LOCATION = "layer-current-location"
private const val SOURCE_ROUTE = "source-route"
private const val LAYER_ROUTE = "layer-route"
private const val SOURCE_VICTIMS = "source-victims"
private const val LAYER_VICTIMS  = "layer-victims"

@Composable
actual fun MapScreen(
    victims: List<VictimRecord>,
    focusedVictimId: String?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val downloadManager = remember { MapDownloadManager(context) }
    val shelterRepository = remember { ShelterRepository(context) }

    var downloadProgress by remember { mutableStateOf(-1) }
    var statusMessage by remember { mutableStateOf("") }
    var mbtilesPath by remember { mutableStateOf("") }
    var mapReady by remember { mutableStateOf(false) }

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLng by remember { mutableStateOf(0.0) }

    var selectedFilter by remember { mutableStateOf(FacilityFilter.ALL) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var sheltersByType by remember { mutableStateOf<Map<ShelterType, List<Shelter>>>(emptyMap()) }
    var filteredShelters by remember { mutableStateOf<List<Shelter>>(emptyList()) }

    // MapboxMapへの参照（GeoJsonSource更新用）
    var mapRef by remember { mutableStateOf<MapboxMap?>(null) }

    val brouterEngine = remember { BRouterEngine(context) }
    val rd5Manager = remember { Rd5DownloadManager(context) }
    var showingRoute by remember { mutableStateOf(false) }
    var calculatingRoute by remember { mutableStateOf(false) }

    // 避難所GeoJSONを更新する関数
    fun updateShelterSource(
        lat: Double,
        lng: Double,
        filter: FacilityFilter,
        byType: Map<ShelterType, List<Shelter>>,
        filtered: List<Shelter>
    ) {
        val map = mapRef ?: return
        val style = map.style ?: return

        (style.getSource(SOURCE_SHELTERS) as? GeoJsonSource)
            ?.setGeoJson(emptyFeatureCollection())

        val shelters: List<Shelter> = when (filter) {
            FacilityFilter.ALL -> byType.values.flatten()
            else -> filtered
        }

        val features = JSONArray()
        shelters.forEach { shelter ->
            val feature = JSONObject().apply {
                put("type", "Feature")
                put("geometry", JSONObject().apply {
                    put("type", "Point")
                    put("coordinates", JSONArray().apply {
                        put(shelter.longitude)
                        put(shelter.latitude)
                    })
                })
                put("properties", JSONObject().apply {
                    put("name", shelter.name)
                    put("icon", shelter.type.iconName())
                })
            }
            features.put(feature)
        }

        val geojson = JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()

        (style.getSource(SOURCE_SHELTERS) as? GeoJsonSource)?.setGeoJson(geojson)

        if (lat != 0.0) {
            val currentLocationGeoJson = JSONObject().apply {
                put("type", "FeatureCollection")
                put("features", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "Feature")
                        put("geometry", JSONObject().apply {
                            put("type", "Point")
                            put("coordinates", JSONArray().apply {
                                put(lng)
                                put(lat)
                            })
                        })
                        put("properties", JSONObject())
                    })
                })
            }.toString()
            (style.getSource(SOURCE_CURRENT_LOCATION) as? GeoJsonSource)
                ?.setGeoJson(currentLocationGeoJson)
        }
    }

    // フィルター変更時
    LaunchedEffect(selectedFilter) {
        if (currentLat == 0.0) return@LaunchedEffect
        when (selectedFilter) {
            FacilityFilter.ALL -> {
                val newData = shelterRepository.getNearestShelters(currentLat, currentLng)
                sheltersByType = newData
                updateShelterSource(currentLat, currentLng, selectedFilter, newData, filteredShelters)
            }
            else -> {
                val shelterType = selectedFilter.toShelterType() ?: return@LaunchedEffect
                val newData = shelterRepository.getNearestSheltersByType(shelterType, currentLat, currentLng)
                filteredShelters = newData
                updateShelterSource(currentLat, currentLng, selectedFilter, sheltersByType, newData)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                initializeMap(
                    context, downloadManager,
                    onStatus = { statusMessage = it },
                    onProgress = { downloadProgress = it },
                    onReady = { path, lat, lng ->
                        mbtilesPath = path
                        currentLat = lat
                        currentLng = lng
                        mapReady = true
                    }
                )
            }
        } else {
            statusMessage = "位置情報の権限が必要です"
        }
    }

    LaunchedEffect(Unit) {
        if (!rd5Manager.areAllSegmentsDownloaded()) {
            rd5Manager.downloadAllSegments { current, total, fileName ->
                android.util.Log.d("MapScreen", "Copying rd5: $fileName ($current/$total)")
            }
        }
        val saved = downloadManager.getDownloadedPrefecture()
        if (saved != null && downloadManager.isMbtilesDownloaded(saved)) {
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFine) {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                var loc = fusedClient.lastLocation.await()
                if (loc == null) {
                    val cts = CancellationTokenSource()
                    loc = fusedClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token
                    ).await()
                }
                currentLat = loc?.latitude ?: 35.1802
                currentLng = loc?.longitude ?: 136.9066
            }
            mbtilesPath = downloadManager.getMbtilesPath(saved)
            mapReady = true
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                initializeMap(
                    context, downloadManager,
                    onStatus = { statusMessage = it },
                    onProgress = { downloadProgress = it },
                    onReady = { path, lat, lng ->
                        mbtilesPath = path
                        currentLat = lat
                        currentLng = lng
                        mapReady = true
                    }
                )
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // mapReady後に避難所データ取得＆ソース更新
    LaunchedEffect(mapReady) {
        if (!mapReady || currentLat == 0.0) return@LaunchedEffect
        val newData = shelterRepository.getNearestShelters(currentLat, currentLng)
        sheltersByType = newData
        updateShelterSource(currentLat, currentLng, FacilityFilter.ALL, newData, filteredShelters)
    }

    // 傷病者にフォーカス
    LaunchedEffect(focusedVictimId) {
        val target = victims.firstOrNull { it.id == focusedVictimId } ?: return@LaunchedEffect
        val lat = target.latitude ?: return@LaunchedEffect
        val lng = target.longitude ?: return@LaunchedEffect
        mapRef?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(lat, lng))
                    .zoom(17.0)
                    .build()
            ), 800
        )
    }

    val mapView = remember {
        Mapbox.getInstance(context)
        MapView(context, MapboxMapOptions.createFromAttributes(context).textureMode(true))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        if (mapReady && mbtilesPath.isNotEmpty()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            mapRef = map
                            val styleJson = buildOfflineStyleJson(mbtilesPath)
                            map.setStyle(Style.Builder().fromJson(styleJson)) { style ->

                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(
                                        currentLat.takeIf { it != 0.0 } ?: 35.1802,
                                        currentLng.takeIf { it != 0.0 } ?: 136.9066
                                    ))
                                    .zoom(13.0)
                                    .build()

                                // アイコン画像をスタイルに登録
                                style.addImage("current-location-icon",
                                    createCircleBitmap(0xFF1565C0.toInt(), sizePx = 48))
                                style.addImage("victim-severe-icon",
                                    createCircleBitmap(0xFFE5463F.toInt(), sizePx = 40))
                                style.addImage("victim-minor-icon",
                                    createCircleBitmap(0xFF16A36B.toInt(), sizePx = 40))
                                style.addImage(ShelterType.EVACUATION_CENTER.iconName(),
                                    createCircleBitmap(0xFF388E3C.toInt()))
                                style.addImage(ShelterType.EMERGENCY_SHELTER.iconName(),
                                    createCircleBitmap(0xFF1976D2.toInt()))
                                style.addImage(ShelterType.FIRST_AID_STATION.iconName(),
                                    createCircleBitmap(0xFFD32F2F.toInt()))
                                style.addImage(ShelterType.HOSPITAL.iconName(),
                                    createCircleBitmap(0xFF7B1FA2.toInt()))

                                // ソースを登録
                                style.addSource(GeoJsonSource(SOURCE_SHELTERS, emptyFeatureCollection()))
                                style.addSource(GeoJsonSource(SOURCE_CURRENT_LOCATION, emptyFeatureCollection()))
                                style.addSource(GeoJsonSource(SOURCE_VICTIMS, buildVictimGeoJson(victims)))

                                // 傷病者レイヤー
                                style.addLayer(
                                    com.mapbox.mapboxsdk.style.layers.SymbolLayer(LAYER_VICTIMS, SOURCE_VICTIMS)
                                        .withProperties(
                                            PropertyFactory.iconImage(
                                                com.mapbox.mapboxsdk.style.expressions.Expression.get("icon")
                                            ),
                                            PropertyFactory.iconSize(1.2f),
                                            PropertyFactory.iconAllowOverlap(true),
                                        )
                                )

                                // 現在地ピンを即時セット
                                if (currentLat != 0.0) {
                                    val currentLocGeoJson = singlePointFeatureCollection(currentLng, currentLat)
                                    (style.getSource(SOURCE_CURRENT_LOCATION) as? GeoJsonSource)
                                        ?.setGeoJson(currentLocGeoJson)
                                }

                                // ルートソースを追加
                                style.addSource(GeoJsonSource(SOURCE_ROUTE, emptyFeatureCollection()))

                                // ルートラインレイヤーを追加
                                style.addLayerBelow(
                                    LineLayer(LAYER_ROUTE, SOURCE_ROUTE).apply {
                                        minZoom = 0f
                                        setProperties(
                                            PropertyFactory.lineColor("#1976D2"),
                                            PropertyFactory.lineWidth(5f),
                                            PropertyFactory.lineOpacity(0.85f),
                                            PropertyFactory.lineCap(
                                                com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
                                            ),
                                            PropertyFactory.lineJoin(
                                                com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
                                            )
                                        )
                                    },
                                    LAYER_SHELTERS
                                )

                                map.addOnMapClickListener { point ->
                                    val screenPoint = map.projection.toScreenLocation(point)
                                    val features = map.queryRenderedFeatures(screenPoint, LAYER_SHELTERS)
                                    if (features.isNotEmpty()) {
                                        val feature = features[0]
                                        val name = feature.getStringProperty("name") ?: ""
                                        val toLat = feature.geometry()?.let {
                                            (it as? com.mapbox.geojson.Point)?.latitude()
                                        } ?: 0.0
                                        val toLng = feature.geometry()?.let {
                                            (it as? com.mapbox.geojson.Point)?.longitude()
                                        } ?: 0.0
                                        if (toLat != 0.0 && currentLat != 0.0) {
                                            scope.launch {
                                                calculatingRoute = true
                                                val route = brouterEngine.calculateRoute(
                                                    currentLat, currentLng, toLat, toLng
                                                )
                                                calculatingRoute = false
                                                if (route != null && route.points.isNotEmpty()) {
                                                    val coords = JSONArray()
                                                    route.points.forEach { (lat, lng) ->
                                                        coords.put(JSONArray().apply {
                                                            put(lng)
                                                            put(lat)
                                                        })
                                                    }
                                                    val routeGeoJson = JSONObject().apply {
                                                        put("type", "FeatureCollection")
                                                        put("features", JSONArray().apply {
                                                            put(JSONObject().apply {
                                                                put("type", "Feature")
                                                                put("geometry", JSONObject().apply {
                                                                    put("type", "LineString")
                                                                    put("coordinates", coords)
                                                                })
                                                                put("properties", JSONObject())
                                                            })
                                                        })
                                                    }.toString()
                                                    val style = map.style
                                                    (style?.getSource(SOURCE_ROUTE) as? GeoJsonSource)
                                                        ?.setGeoJson(routeGeoJson)
                                                    showingRoute = true
                                                } else {
                                                    android.util.Log.w("MapScreen", "Route not found to $name")
                                                }
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        }
                    }
                }
            )

            // 右上：フィルタープルダウン
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                FilledTonalButton(onClick = { dropdownExpanded = true }) {
                    Text(text = selectedFilter.displayName, fontSize = 13.sp)
                    Text(text = " ▼", fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    FacilityFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = filter.displayName,
                                    color = if (filter == selectedFilter) Color(0xFF1976D2)
                                    else Color.Unspecified
                                )
                            },
                            onClick = {
                                selectedFilter = filter
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 左上：ルート解除ボタン（ルート表示中のみ）
            if (showingRoute) {
                FilledTonalButton(
                    onClick = {
                        showingRoute = false
                        val style = mapRef?.style
                        (style?.getSource(SOURCE_ROUTE) as? GeoJsonSource)
                            ?.setGeoJson(emptyFeatureCollection())
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text("✕ ルート解除", fontSize = 13.sp)
                }
            }
        }

        // ルート計算中オーバーレイ
        if (calculatingRoute) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ルートを計算中...", color = Color.White)
                }
            }
        }

        // ダウンロード中オーバーレイ
        if (downloadProgress in 0..100 || statusMessage.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (downloadProgress in 0..100) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$statusMessage ($downloadProgress%)",
                            color = Color.White
                        )
                    } else {
                        Text(text = statusMessage, color = Color.White)
                    }
                }
            }
        }
    }
}

// ShelterType のアイコン名
private fun ShelterType.iconName(): String = when (this) {
    ShelterType.EVACUATION_CENTER  -> "evacuation-center-icon"
    ShelterType.EMERGENCY_SHELTER  -> "emergency-shelter-icon"
    ShelterType.FIRST_AID_STATION  -> "first-aid-icon"
    ShelterType.HOSPITAL           -> "hospital-icon"
}

// FacilityFilter → ShelterType 変換
private fun FacilityFilter.toShelterType(): ShelterType? = when (this) {
    FacilityFilter.EVACUATION_CENTER  -> ShelterType.EVACUATION_CENTER
    FacilityFilter.EMERGENCY_SHELTER  -> ShelterType.EMERGENCY_SHELTER
    FacilityFilter.FIRST_AID_STATION  -> ShelterType.FIRST_AID_STATION
    FacilityFilter.HOSPITAL           -> ShelterType.HOSPITAL
    FacilityFilter.ALL                -> null
}

private fun emptyFeatureCollection(): String =
    """{"type":"FeatureCollection","features":[]}"""

private fun singlePointFeatureCollection(lng: Double, lat: Double): String =
    """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[$lng,$lat]},"properties":{}}]}"""

private fun buildVictimGeoJson(victims: List<VictimRecord>): String {
    val features = victims.mapNotNull { v ->
        val lat = v.latitude ?: return@mapNotNull null
        val lng = v.longitude ?: return@mapNotNull null
        val icon = if (v.result == TriageResult.SEVERE) "victim-severe-icon" else "victim-minor-icon"
        """{"type":"Feature","properties":{"id":"${v.id}","icon":"$icon","no":${v.displayNo}},"geometry":{"type":"Point","coordinates":[$lng,$lat]}}"""
    }
    return """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""
}

/** 色付き円Bitmapを生成 */
private fun createCircleBitmap(color: Int, sizePx: Int = 36): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.1f
    }
    canvas.drawCircle(center, center, center * 0.8f, fill)
    canvas.drawCircle(center, center, center * 0.8f, stroke)
    return bitmap
}

private suspend fun initializeMap(
    context: Context,
    downloadManager: MapDownloadManager,
    onStatus: (String) -> Unit,
    onProgress: (Int) -> Unit,
    onReady: (String, Double, Double) -> Unit
) {
    onStatus("現在地を取得中...")

    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFineLocation && !hasCoarseLocation) {
        onStatus("位置情報の権限がありません")
        return
    }

    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        var location = fusedClient.lastLocation.await()
        if (location == null) {
            val cts = CancellationTokenSource()
            location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token
            ).await()
        }

        val lat = location?.latitude ?: 35.1802
        val lng = location?.longitude ?: 136.9066

        val prefecture = downloadManager.getPrefectureFromLocation(lat, lng)
        val fileName = prefecture?.let { getPrefectureFileName(it) }

        if (fileName == null) {
            onStatus("県の特定に失敗しました")
            return
        }

        onStatus("${prefecture}のマップをダウンロード中...")
        onProgress(0)

        val success = downloadManager.downloadMbtiles(fileName) { progress ->
            onProgress(progress)
        }

        onProgress(-1)

        if (success) {
            onStatus("")
            onReady(downloadManager.getMbtilesPath(fileName), lat, lng)
        } else {
            onStatus("ダウンロードに失敗しました。\nネット接続を確認してください。")
        }
    } catch (e: Exception) {
        onStatus("エラーが発生しました: ${e.message}")
    }
}

private fun buildOfflineStyleJson(mbtilesPath: String): String {
    return """
    {
      "version": 8,
      "name": "Offline",
      "sources": {
        "aichi": {
          "type": "vector",
          "url": "mbtiles:///MBTILES_PATH"
        }
      },
      "layers": [
        { "id": "background", "type": "background", "paint": { "background-color": "#f5f5f0" } },
        { "id": "water", "type": "fill", "source": "aichi", "source-layer": "water", "paint": { "fill-color": "#88c0d8" } },
        { "id": "waterway", "type": "line", "source": "aichi", "source-layer": "waterway", "paint": { "line-color": "#88c0d8", "line-width": 1.5 } },
        { "id": "landcover-grass", "type": "fill", "source": "aichi", "source-layer": "landcover", "filter": ["in", "class", "grass", "scrub"], "paint": { "fill-color": "#d8e8c8" } },
        { "id": "landcover-wood", "type": "fill", "source": "aichi", "source-layer": "landcover", "filter": ["==", "class", "wood"], "paint": { "fill-color": "#b8d8a0" } },
        { "id": "landuse-residential", "type": "fill", "source": "aichi", "source-layer": "landuse", "filter": ["==", "class", "residential"], "paint": { "fill-color": "#ededea" } },
        { "id": "park", "type": "fill", "source": "aichi", "source-layer": "park", "paint": { "fill-color": "#c8e0b0" } },
        { "id": "building", "type": "fill", "source": "aichi", "source-layer": "building", "minzoom": 13, "paint": { "fill-color": "#d8d0c0", "fill-outline-color": "#c0b8a8" } },
        { "id": "road-minor", "type": "line", "source": "aichi", "source-layer": "transportation", "filter": ["in", "class", "minor", "service"], "paint": { "line-color": "#ffffff", "line-width": ["interpolate", ["linear"], ["zoom"], 10, 1, 14, 3] } },
        { "id": "road-secondary", "type": "line", "source": "aichi", "source-layer": "transportation", "filter": ["in", "class", "secondary", "tertiary"], "paint": { "line-color": "#f5f0e8", "line-width": ["interpolate", ["linear"], ["zoom"], 8, 1, 14, 5] } },
        { "id": "road-primary", "type": "line", "source": "aichi", "source-layer": "transportation", "filter": ["==", "class", "primary"], "paint": { "line-color": "#ffd080", "line-width": ["interpolate", ["linear"], ["zoom"], 6, 1, 14, 8] } },
        { "id": "road-motorway", "type": "line", "source": "aichi", "source-layer": "transportation", "filter": ["==", "class", "motorway"], "paint": { "line-color": "#e06020", "line-width": ["interpolate", ["linear"], ["zoom"], 5, 1.5, 14, 12] } },
        { "id": "railway", "type": "line", "source": "aichi", "source-layer": "transportation", "filter": ["in", "class", "rail", "transit"], "paint": { "line-color": "#a080c0", "line-width": 2, "line-dasharray": [3, 1] } },
        { "id": "boundary", "type": "line", "source": "aichi", "source-layer": "boundary", "paint": { "line-color": "#a0a0c0", "line-width": 1, "line-dasharray": [4, 2] } },
        { "id": "place-town", "type": "symbol", "source": "aichi", "source-layer": "place", "filter": ["in", "class", "town", "city"], "layout": { "text-field": "{name:latin}", "text-size": 13 }, "paint": { "text-color": "#303030", "text-halo-color": "#ffffff", "text-halo-width": 2 } },
        { "id": "place-village", "type": "symbol", "source": "aichi", "source-layer": "place", "filter": ["in", "class", "village", "suburb"], "minzoom": 11, "layout": { "text-field": "{name:latin}", "text-size": 11 }, "paint": { "text-color": "#505050", "text-halo-color": "#ffffff", "text-halo-width": 1.5 } },
        {
          "id": "layer-shelters",
          "type": "symbol",
          "source": "source-shelters",
          "minzoom": 0,
          "maxzoom": 24,
          "layout": {
            "icon-image": ["get", "icon"],
            "icon-size": 1.2,
            "icon-allow-overlap": true,
            "icon-ignore-placement": true
          }
        },
        {
          "id": "layer-shelter-labels",
          "type": "symbol",
          "source": "source-shelters",
          "minzoom": 12,
          "maxzoom": 24,
          "layout": {
            "text-field": ["get", "name"],
            "text-size": 13,
            "text-offset": [0, 1.5],
            "text-allow-overlap": false,
            "text-ignore-placement": false
          },
          "paint": {
            "text-color": "#333333",
            "text-halo-color": "#FFFFFF",
            "text-halo-width": 1.5
          }
        },
        {
          "id": "layer-current-location",
          "type": "symbol",
          "source": "source-current-location",
          "minzoom": 0,
          "maxzoom": 24,
          "layout": {
            "icon-image": "current-location-icon",
            "icon-size": 1.5,
            "icon-allow-overlap": true,
            "icon-ignore-placement": true
          }
        }
      ]
    }
    """.trimIndent().replace("MBTILES_PATH", mbtilesPath)
}