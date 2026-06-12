package com.urg.edge.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.urg.edge.map.MapDownloadManager
import com.urg.edge.map.getPrefectureFileName
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
actual fun MapScreen(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val downloadManager = remember { MapDownloadManager(context) }

    var downloadProgress by remember { mutableStateOf(-1) }
    var statusMessage by remember { mutableStateOf("") }
    var mbtilesPath by remember { mutableStateOf("") }
    var mapReady by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                initializeMap(
                    context, downloadManager,
                    onStatus = { statusMessage = it },
                    onProgress = { downloadProgress = it },
                    onReady = { path ->
                        mbtilesPath = path
                        mapReady = true
                    }
                )
            }
        } else {
            statusMessage = "位置情報の権限が必要です"
        }
    }

    LaunchedEffect(Unit) {
        val saved = downloadManager.getDownloadedPrefecture()
        if (saved != null && downloadManager.isMbtilesDownloaded(saved)) {
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
                    onReady = { path ->
                        mbtilesPath = path
                        mapReady = true
                    }
                )
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
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
                            val styleJson = buildOfflineStyleJson(mbtilesPath)
                            map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(35.1802, 136.9066))
                                    .zoom(10.0)
                                    .build()

                                style.addImage(
                                    "pin-icon",
                                    BitmapUtils.getBitmapFromDrawable(
                                        context.resources.getDrawable(
                                            android.R.drawable.ic_menu_mylocation, null
                                        )
                                    )!!
                                )

                                val symbolManager = SymbolManager(mapView, map, style)
                                symbolManager.iconAllowOverlap = true
                                symbolManager.iconIgnorePlacement = true
                                symbolManager.create(
                                    SymbolOptions()
                                        .withLatLng(LatLng(35.1802, 136.9066))
                                        .withIconImage("pin-icon")
                                        .withIconSize(2.0f)
                                        .withTextField("名古屋駅")
                                        .withTextOffset(arrayOf(0f, 1.5f))
                                        .withTextColor("#FF0000")
                                )
                            }
                        }
                    }
                }
            )
        }

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

private suspend fun initializeMap(
    context: Context,
    downloadManager: MapDownloadManager,
    onStatus: (String) -> Unit,
    onProgress: (Int) -> Unit,
    onReady: (String) -> Unit
) {
    onStatus("現在地を取得中...")

    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        // lastLocation が null の場合は currentLocation を試みる
        var location = fusedClient.lastLocation.await()
        if (location == null) {
            val cts = CancellationTokenSource()
            location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
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
            onReady(downloadManager.getMbtilesPath(fileName))
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
        { "id": "place-village", "type": "symbol", "source": "aichi", "source-layer": "place", "filter": ["in", "class", "village", "suburb"], "minzoom": 11, "layout": { "text-field": "{name:latin}", "text-size": 11 }, "paint": { "text-color": "#505050", "text-halo-color": "#ffffff", "text-halo-width": 1.5 } }
      ]
    }
    """.trimIndent().replace("MBTILES_PATH", mbtilesPath)
}