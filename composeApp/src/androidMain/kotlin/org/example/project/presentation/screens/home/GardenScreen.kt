package org.example.project.presentation.screens.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import org.example.project.data.dogGardens.DogGardensViewModel
import org.example.project.features.registration.UserViewModel
import org.example.project.presentation.screens.HomeTab
import org.example.project.utils.Location

// constants for fixed text widths
private val thirtySixDp = 36.dp
private val fortyEightDp = 48.dp

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    viewModel: DogGardensViewModel,
    onBack: () -> Unit,
    onScan: () -> Unit, // kept for compatibility
    onGoProfile: () -> Unit,
    userViewModel: UserViewModel
) {
    // Permissions (delegated effect)
    val fine = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    LocationPermissionGate(
        fine = fine,
        coarse = coarse,
        onGranted = { viewModel.loadLocation() }
    )

    var selectedTab by remember { mutableStateOf(HomeTab.Yard) }

    // Observe VM state
    val userLoc by viewModel.userLocation.collectAsState()
    val searchCenter by viewModel.searchCenter.collectAsState()
    val gardens by viewModel.gardens.collectAsState()
    val radiusMeters by viewModel.radiusMeters.collectAsState()
    val radiusKm = (radiusMeters / 1000f).coerceAtLeast(1f)

    // Selected garden (sheet host will handle side-effects)
    var selectedGarden by rememberSaveable { mutableStateOf<org.example.project.domain.models.DogGarden?>(null) }

    // Dog picker state
    val showPicker by userViewModel.shouldShowDogPicker.collectAsState()
    val cachedUser by userViewModel.currentUser.collectAsState()
    val selectedDogIds by userViewModel.selectedDogIds.collectAsState()

    // Ensure we have user/dogs (picker)
    LaunchedEffect(Unit) { userViewModel.loadUserIfNeeded() }

    // Keep garden list in sync with radius/center
    LaunchedEffect(radiusMeters, searchCenter) { viewModel.getGoogleGardens() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = tab.icon,
                        label = { Text(tab.label) },
                        selected = (tab == selectedTab),
                        onClick = {
                            selectedTab = tab
                            if (tab == HomeTab.Profile) onGoProfile()
                        }
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
            TopInfoPanel(
                gardenCount = gardens.size,
                radiusKm = radiusKm,
                onRadiusCommit = { meters ->
                    viewModel.setRadius(meters)
                    viewModel.getGoogleGardens()
                }
            )

            // Map / loading
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
                        userLoc = userLoc,
                        onMarkerClick = { g -> selectedGarden = g }
                    )
                }
            }

            // Scan button (as you had)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.useMyLocationAsCenter(); viewModel.getGoogleGardens() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Use my location")
                }
            }
        }
    }

    // Dog picker host
    DogPickerHost(
        showPicker = showPicker,
        userViewModel = userViewModel,
        dogs = cachedUser?.dogList ?: emptyList(),
    )

    // Garden sheet host (loads photo + presence + check-in/out)
    val myDogs = remember(cachedUser, selectedDogIds) {
        (cachedUser?.dogList ?: emptyList()).filter { it.id in selectedDogIds }
    }
    GardenBottomSheetHost(
        selectedGarden = selectedGarden,
        onDismiss = { selectedGarden = null },
        viewModel = viewModel,
        myDogs = myDogs
    )

    BackHandler { onBack() }
}

/* ---------- Small helpers to reduce complexity ---------- */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermissionGate(
    fine: com.google.accompanist.permissions.PermissionState,
    coarse: com.google.accompanist.permissions.PermissionState,
    onGranted: () -> Unit
) {
    var requestedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(fine.status.isGranted, coarse.status.isGranted) {
        when {
            !fine.status.isGranted && !coarse.status.isGranted && !requestedOnce -> {
                requestedOnce = true
                fine.launchPermissionRequest()
            }
            fine.status.isGranted || coarse.status.isGranted -> onGranted()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopInfoPanel(
    gardenCount: Int,
    radiusKm: Float,
    onRadiusCommit: (Int) -> Unit
) {
    var sliderKm by rememberSaveable { mutableStateOf(radiusKm) }
    LaunchedEffect(radiusKm) { sliderKm = radiusKm }

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
            Text("Dog Parks Found: $gardenCount", style = MaterialTheme.typography.titleMedium)
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
                    value = sliderKm,
                    onValueChange = { sliderKm = it },
                    valueRange = 1f..10f,
                    steps = 9,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    onValueChangeFinished = {
                        onRadiusCommit((sliderKm * 1000).toInt())
                    }
                )
                Text(
                    text = "10 km",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(fortyEightDp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DogPickerHost(
    showPicker: Boolean,
    userViewModel: UserViewModel,
    dogs: List<org.example.project.data.remote.dto.DogDto>
) {
    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (!showPicker) return

    ModalBottomSheet(
        onDismissRequest = { userViewModel.dismissDogPicker() },
        sheetState = pickerSheetState
    ) {
        DogMultiSelectSheet(
            dogs = dogs,
            initiallySelected = userViewModel.selectedDogIds.collectAsState().value,
            onConfirm = { selectedIds ->
                userViewModel.setSelectedDogIds(selectedIds)
                userViewModel.dismissDogPicker()
            },
            onCancel = { userViewModel.dismissDogPicker() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GardenBottomSheetHost(
    selectedGarden: org.example.project.domain.models.DogGarden?,
    onDismiss: () -> Unit,
    viewModel: DogGardensViewModel,
    myDogs: List<org.example.project.data.remote.dto.DogDto>
) {
    if (selectedGarden == null) {
        // Ensure we stop listening if the sheet is closed externally
        LaunchedEffect(Unit) { viewModel.stopWatchingPresence() }
        return
    }

    val photoUrl by viewModel.gardenPhotoUrl.collectAsState()
    val presentDogs by viewModel.presentDogs.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiScope = rememberCoroutineScope()

    // Start/stop presence + load photo when garden changes
    LaunchedEffect(selectedGarden.id) {
        viewModel.loadGardenPhoto(selectedGarden.id, 900)
        viewModel.startWatchingPresence(selectedGarden.id)
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.stopWatchingPresence()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        GardenInfoSheet(
            garden = selectedGarden,
            photoUrl = photoUrl,
            presentDogs = presentDogs,
            mySelectedDogs = myDogs,
            onCheckIn = {
                uiScope.launch {
                    viewModel.checkInDogs(selectedGarden.id, myDogs)
                }
            },
            onCheckOut = {
                uiScope.launch {
                    viewModel.checkOutDogs(selectedGarden.id, myDogs.map { it.id })
                }
            }
        )
    }
}

/* ------------ Existing pieces (unchanged) ------------- */

@Composable
private fun MapView(
    center: Location,
    parks: List<org.example.project.domain.models.DogGarden>,
    radiusMeters: Int,
    userLoc: Location?,
    onMarkerClick: (org.example.project.domain.models.DogGarden) -> Unit
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
                title = park.name,
                onClick = {
                    onMarkerClick(park)
                    true
                }
            )
        }
    }
}

@Composable
private fun GardenInfoSheet(
    garden: org.example.project.domain.models.DogGarden,
    photoUrl: String?,
    presentDogs: List<org.example.project.data.remote.dto.DogDto>,
    mySelectedDogs: List<org.example.project.data.remote.dto.DogDto>,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(garden.name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (!photoUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = photoUrl,
                    contentDescription = garden.name,
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photo")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Dogs here now (${presentDogs.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (presentDogs.isEmpty()) {
            Text("Be the first to check in!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presentDogs, key = { it.id }) { dog ->
                    DogRowCompact(dog)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (mySelectedDogs.isNotEmpty()) {
            Text(
                "You selected ${mySelectedDogs.size} dog(s) for check-in",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onCheckOut) { Text("Check out") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onCheckIn, enabled = mySelectedDogs.isNotEmpty()) { Text("Check in") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DogRowCompact(dog: org.example.project.data.remote.dto.DogDto) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (dog.photoUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = dog.photoUrl,
                    contentDescription = dog.name,
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🐶") }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(dog.name.ifBlank { "Dog" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DogMultiSelectSheet(
    dogs: List<org.example.project.data.remote.dto.DogDto>,
    initiallySelected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onCancel: () -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("WHO ARE YOU PUP FIESTING WITH?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (dogs.isEmpty()) {
            Text(
                "No dogs found for your profile.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(dogs, key = { it.id }) { dog ->
                    DogSelectRow(
                        dog = dog,
                        checked = dog.id in selected,
                        onToggle = {
                            selected = if (dog.id in selected) selected - dog.id else selected + dog.id
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(selected) }, enabled = dogs.isNotEmpty()) { Text("Confirm") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DogSelectRow(
    dog: org.example.project.data.remote.dto.DogDto,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(56.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (dog.photoUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = dog.photoUrl,
                    contentDescription = dog.name,
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🐶") }
            }
        }

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(dog.name.ifBlank { "Dog" }, style = MaterialTheme.typography.bodyLarge)
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

