package org.example.project.presentation.screens.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.cancel
import org.example.project.data.dogGardens.DogGardensViewModel
import org.example.project.platformLogger
import org.example.project.utils.Location

private enum class HomeTab(val label: String, val icon: @Composable () -> Unit) {
    Yard("Yard", { Icon(Icons.Filled.Pets, contentDescription = "Yard") }),
    Profile("Profile", { Icon(Icons.Filled.Person, contentDescription = "Profile") })
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    viewModel: DogGardensViewModel,
    onBack: () -> Unit,
    onScan: () -> Unit // kept for compatibility (not used below)
) {
    // ---- Permission handling (single, consolidated) ----
    val fine = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    var requestedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(fine.status.isGranted, coarse.status.isGranted) {
        when {
            !fine.status.isGranted && !coarse.status.isGranted && !requestedOnce -> {
                requestedOnce = true
                fine.launchPermissionRequest() // user may grant approximate => COARSE
            }
            fine.status.isGranted || coarse.status.isGranted -> {
                viewModel.loadLocation()
            }
        }
    }

    var selectedTab by remember { mutableStateOf(HomeTab.Yard) }

    // ---- Observe VM state ----
    val userLoc by viewModel.userLocation.collectAsState()
    val searchCenter by viewModel.searchCenter.collectAsState()
    val gardens by viewModel.gardens.collectAsState()
    val radiusMeters by viewModel.radiusMeters.collectAsState()
    val radiusKm = (radiusMeters / 1000f).coerceAtLeast(1f)

    // Persist when radius or location changes
    LaunchedEffect(radiusMeters, userLoc) {
        if (userLoc != null) {
           // viewModel.refreshAndSave()
                viewModel.getGardens()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = tab.icon,
                        label = { Text(tab.label) },
                        selected = (tab == selectedTab),
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top info strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Dog Parks Found: ${gardens.size}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Radius: ${radiusKm.toInt()} km", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "1 km",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(thirtySixDp)
                        )
                        Slider(
                            value = radiusKm,
                            onValueChange = { newKm ->
                                viewModel.setRadius((newKm * 1000).toInt())
                            },
                            valueRange = 1f..10f,
                            steps = 49,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "10 km",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(fortyEightDp)
                        )
                    }
                }
            }

            // Map or loading
            Box(modifier = Modifier.weight(1f)) {
                if (userLoc == null) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Waiting for location…",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    MapView(
                        center = searchCenter,
                        parks = gardens,
                        radiusMeters = radiusMeters,
                        userLoc = userLoc
                    )
                }
            }

            // Scan button
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.onScanClick() }, // keep UI dumb, VM does the work
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Scan")
                }
            }
        }
    }

    BackHandler { onBack() }

    // Clean up VM scope when screen is popped
    DisposableEffect(Unit) {
        onDispose { viewModel.scope.cancel() }
    }
}

@Composable
private fun MapView(
    center: Location,
    parks: List<org.example.project.domain.models.DogGarden>,
    radiusMeters: Int,
    userLoc: Location?
) {
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(center.latitude, center.longitude), 14f)
    }

    LaunchedEffect(center) {
        cameraState.position = CameraPosition.fromLatLngZoom(
            LatLng(center.latitude, center.longitude),
            14f
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraState
    ) {
        com.google.maps.android.compose.Circle(
            center = LatLng(center.latitude, center.longitude),
            radius = radiusMeters.toDouble(),
            strokeWidth = 2f
        )

        userLoc?.let {
            Marker(
                state = rememberMarkerState(position = LatLng(it.latitude, it.longitude)),
                title = "You are here"
            )
        }

        parks.forEach { park ->
            Marker(
                state = rememberMarkerState(
                    position = LatLng(park.location.latitude, park.location.longitude)
                ),
                title = park.name
            )
        }
    }
}

// constants for fixed text widths
private val thirtySixDp = 36.dp
private val fortyEightDp = 48.dp