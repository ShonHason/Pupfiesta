// androidMain/src/main/kotlin/org/example/project/presentation/screens/home/GardenScreen.kt
package org.example.project.presentation.screens.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
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
import org.example.project.features.dogGardens.DogGardensViewModel
import org.example.project.utils.Location

private enum class HomeTab(val label: String, val icon: @Composable () -> Unit) {
    Yard("Yard",    { Icon(Icons.Filled.Pets, contentDescription = "Yard") }),
    Profile("Profile", { Icon(Icons.Filled.Person, contentDescription = "Profile") })
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    viewModel: DogGardensViewModel,
    onBack: () -> Unit,
    onScan: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(HomeTab.Yard) }

    // 1️⃣ Handle location permission
    val locationPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var hasAskedPermission by remember { mutableStateOf(false) }
    LaunchedEffect(locationPerm.status) {
        if (!locationPerm.status.isGranted && !hasAskedPermission) {
            locationPerm.launchPermissionRequest()
            hasAskedPermission = true
        } else if (locationPerm.status.isGranted) {
            viewModel.loadLocation()
        }
    }

    // 2️⃣ Observe state from VM
    val userLoc     by viewModel.userLocation.collectAsState()
    val gardens     by viewModel.gardens.collectAsState()
    val radiusMeters by viewModel.radiusMeters.collectAsState()
    val radiusKm    = (radiusMeters / 1000f).coerceAtLeast(1f)

    // 3️⃣ When radius changes, re-fetch
    LaunchedEffect(radiusMeters, userLoc) {
        if (userLoc != null) {

            viewModel.getGardens()
            viewModel.saveGardens()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon    = tab.icon,
                        label   = { Text(tab.label) },
                        selected= (tab == selectedTab),
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
            // — Top info strip with dynamic radius and count —
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Dog Parks Found: ${gardens.size}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Radius: ${radiusKm.toInt()} km", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    // Slider to adjust radius between 1 km and 50 km
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "1 km",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width( thirtySixDp )
                        )
                        Slider(
                            value = radiusKm,
                            onValueChange = { newKm ->
                                viewModel.setRadius((newKm * 1000).toInt())
                            },
                            valueRange = 1f..50f,
                            steps = 49,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "50 km",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width( fortyEightDp )
                        )
                    }
                }
            }

            // — Map or loading message —
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
                    MapView(userLoc!!)
                }
            }

            // — Scan button —
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Scan")
                }
            }
        }
    }

    // 4️⃣ Back‐handler
    BackHandler { onBack() }

    // 5️⃣ Clean up VM’s scope when this screen is popped
    DisposableEffect(Unit) {
        onDispose { viewModel.scope.cancel() }
    }
}

@Composable
private fun MapView(loc: Location) {
    // Tel Aviv’s coordinates as fallback center
    val center = LatLng(32.0853, 34.7818)

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraState
    ) {
        // Marker at Tel Aviv (or user location if you prefer)
        Marker(
            state = rememberMarkerState(position = center),
            title = "You are here"
        )
    }
}

// constants for fixed text widths
private val thirtySixDp = 36.dp
private val fortyEightDp = 48.dp
