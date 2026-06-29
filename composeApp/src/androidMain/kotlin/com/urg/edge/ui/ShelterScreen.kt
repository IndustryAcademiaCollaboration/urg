package com.urg.edge.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.urg.edge.map.Rd5DownloadManager
import com.urg.edge.routing.BRouterEngine
import com.urg.edge.routing.RouteResult
import com.urg.edge.shelter.Shelter
import com.urg.edge.shelter.ShelterRepository
import com.urg.edge.shelter.ShelterType
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

@Composable
fun ShelterScreen(
    modifier: Modifier = Modifier,
    onRouteSelected: (RouteResult, Shelter) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shelterRepository = remember { ShelterRepository(context) }
    val brouterEngine = remember { BRouterEngine(context) }
    val rd5Manager = remember { Rd5DownloadManager(context) }

    var selectedType by remember { mutableStateOf<ShelterType?>(null) }
    var sheltersByType by remember { mutableStateOf<Map<ShelterType, List<Shelter>>>(emptyMap()) }
    var filteredShelters by remember { mutableStateOf<List<Shelter>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLng by remember { mutableStateOf(0.0) }
    var calculatingRoute by remember { mutableStateOf(false) }

    suspend fun loadShelters(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng
        isLoading = true
        statusMessage = "避難所データを読み込み中..."
        if (selectedType == null) {
            sheltersByType = shelterRepository.getNearestShelters(lat, lng)
        } else {
            filteredShelters = shelterRepository.getNearestSheltersByType(selectedType!!, lat, lng)
        }
        isLoading = false
        statusMessage = ""
    }

    suspend fun initialize(context: Context) {
        isLoading = true

        // .rd5セグメントファイルがなければダウンロード
        if (!rd5Manager.areAllSegmentsDownloaded()) {
            statusMessage = "ルートデータをダウンロード中..."
            val success = rd5Manager.downloadAllSegments { current, total, fileName ->
                statusMessage = "ルートデータをダウンロード中...\n$fileName ($current/$total)"
            }
            if (!success) {
                statusMessage = "ルートデータのダウンロードに失敗しました"
                isLoading = false
                return
            }
        }

        statusMessage = "現在地を取得中..."
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            statusMessage = "位置情報の権限がありません"
            isLoading = false
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
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
            loadShelters(lat, lng)
        } catch (e: Exception) {
            statusMessage = "位置情報の取得に失敗しました"
            isLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { initialize(context) }
        } else {
            statusMessage = "位置情報の権限が必要です"
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            initialize(context)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(selectedType) {
        if (currentLat != 0.0 && !isLoading) {
            loadShelters(currentLat, currentLng)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        ShelterTypeFilterBar(
            selectedType = selectedType,
            onTypeSelected = { selectedType = it }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = statusMessage, fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else if (statusMessage.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = statusMessage, color = Color.Red)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (selectedType == null) {
                    ShelterType.entries.forEach { type ->
                        val shelters = sheltersByType[type] ?: emptyList()
                        if (shelters.isNotEmpty()) {
                            item { ShelterSectionHeader(type = type) }
                            items(shelters) { shelter ->
                                ShelterListItem(
                                    shelter = shelter,
                                    onRouteClick = {
                                        scope.launch {
                                            calculatingRoute = true
                                            val route = brouterEngine.calculateRoute(
                                                currentLat, currentLng,
                                                shelter.latitude, shelter.longitude
                                            )
                                            calculatingRoute = false
                                            if (route != null) onRouteSelected(route, shelter)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    items(filteredShelters) { shelter ->
                        ShelterListItem(
                            shelter = shelter,
                            onRouteClick = {
                                scope.launch {
                                    calculatingRoute = true
                                    val route = brouterEngine.calculateRoute(
                                        currentLat, currentLng,
                                        shelter.latitude, shelter.longitude
                                    )
                                    calculatingRoute = false
                                    if (route != null) onRouteSelected(route, shelter)
                                }
                            }
                        )
                    }
                }
            }
        }

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
    }
}

@Composable
fun ShelterTypeFilterBar(
    selectedType: ShelterType?,
    onTypeSelected: (ShelterType?) -> Unit
) {
    val typeColors = mapOf(
        ShelterType.EMERGENCY_SHELTER to Color(0xFF1976D2),
        ShelterType.EVACUATION_CENTER to Color(0xFF388E3C),
        ShelterType.FIRST_AID_STATION to Color(0xFFD32F2F),
        ShelterType.HOSPITAL to Color(0xFF7B1FA2)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("すべて", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
        )
        ShelterType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(if (selectedType == type) null else type) },
                label = { Text(type.displayName, fontSize = 11.sp) },
                modifier = Modifier.weight(1.5f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = typeColors[type] ?: Color.Gray,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun ShelterSectionHeader(type: ShelterType) {
    val typeColors = mapOf(
        ShelterType.EMERGENCY_SHELTER to Color(0xFF1976D2),
        ShelterType.EVACUATION_CENTER to Color(0xFF388E3C),
        ShelterType.FIRST_AID_STATION to Color(0xFFD32F2F),
        ShelterType.HOSPITAL to Color(0xFF7B1FA2)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(typeColors[type] ?: Color.Gray)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = type.displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ShelterListItem(
    shelter: Shelter,
    onRouteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRouteClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = shelter.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDistance(shelter.distanceMeters),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Text(text = "›", fontSize = 20.sp, color = Color.Gray)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) "${meters.roundToInt()}m"
    else "${"%.1f".format(meters / 1000)}km"
}