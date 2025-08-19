package org.example.project.presentation.screens.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import org.example.project.CloudinaryUploader
import org.example.project.data.dogs.DogsViewModel
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.features.registration.UserViewModel
import org.example.project.presentation.screens.HomeTab
import org.koin.compose.koinInject
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onGoYard: () -> Unit,
    onAddDog: () -> Unit
) {

    val dogsVM = runCatching { koinInject<DogsViewModel>() }.getOrElse { DogsViewModel() }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity


    val hasCamera by remember {
        mutableStateOf(
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        )
    }

    // Load/Observe user
    var user by remember { mutableStateOf<UserDto?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        when (val r = viewModel.loadUserIfNeeded()) {
            is Result.Success -> errorText = null
            is Result.Failure -> errorText = r.error?.message ?: "Failed to load profile"
        }
    }
    val cachedUser by viewModel.currentUser.collectAsState()
    LaunchedEffect(cachedUser) { user = cachedUser }

    val dogs = user?.dogList ?: emptyList()
    var selectedTab by remember { mutableStateOf(HomeTab.Profile) }

    // Selection & editor state
    var selectedDogId by remember { mutableStateOf<String?>(null) }
    var editedDog by remember(selectedDogId, dogs) {
        mutableStateOf(dogs.firstOrNull { it.id == selectedDogId })
    }
    var weightField by remember(editedDog?.id) { mutableStateOf(editedDog?.weight?.toString().orEmpty()) }
    var friendly by remember(editedDog?.id) { mutableStateOf(editedDog?.isFriendly ?: false) }
    var neutered by remember(editedDog?.id) { mutableStateOf(editedDog?.isNeutered ?: false) }
    var uploading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Photo source chooser
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    // If the user tapped "Don’t ask again", show a Settings dialog
    var showOpenSettings by remember { mutableStateOf(false) }

    // Launchers
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val dog = editedDog ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        CloudinaryUploader.upload(context, uri) { url ->
            uploading = false
            if (url.isNullOrBlank()) {
                scope.launch { snackbar.showSnackbar("Photo upload failed") }
            } else {
                val toSave = dog.copy(dogPictureUrl = url)
                scope.launch {
                    when (dogsVM.updateDogAndUser(toSave)) {
                        is Result.Success -> {
                            editedDog = toSave
                            viewModel.invalidateUserCache()
                            viewModel.loadUserIfNeeded()
                            snackbar.showSnackbar("Photo updated")
                        }
                        is Result.Failure -> snackbar.showSnackbar("Failed to update photo")
                    }
                }
            }
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val dog = editedDog ?: return@rememberLauncherForActivityResult
        if (bitmap == null) {
            scope.launch { snackbar.showSnackbar("No photo captured") }
            return@rememberLauncherForActivityResult
        }
        uploading = true
        CloudinaryUploader.upload(context, bitmap) { url ->
            uploading = false
            if (url.isNullOrBlank()) {
                scope.launch { snackbar.showSnackbar("Photo upload failed") }
            } else {
                val toSave = dog.copy(dogPictureUrl = url)
                scope.launch {
                    when (dogsVM.updateDogAndUser(toSave)) {
                        is Result.Success -> {
                            editedDog = toSave
                            viewModel.invalidateUserCache()
                            viewModel.loadUserIfNeeded()
                            snackbar.showSnackbar("Photo updated")
                        }
                        is Result.Failure -> snackbar.showSnackbar("Failed to update photo")
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        } else {
            val permanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            if (permanentlyDenied) {
                showOpenSettings = true
            } else {
                scope.launch { snackbar.showSnackbar("Camera permission is required to take a photo") }
            }
        }
    }


    val editorState = editedDog?.let {
        DogEditorUiState(
            dog = it,
            weightField = weightField,
            friendly = friendly,
            neutered = neutered,
            uploading = uploading,
            saving = saving
        )
    }

    val editorActions = DogEditorActions(
        onWeightChange = { s -> weightField = s.filter { ch -> ch.isDigit() }.take(3) },
        onToggleFriendly = { friendly = !friendly },
        onToggleNeutered = { neutered = !neutered },
        onPickPhoto = { showPhotoSourceSheet = true }, // open chooser sheet
        onSave = {
            val base = editedDog ?: return@DogEditorActions
            val newWeight = weightField.toIntOrNull() ?: 0
            val toSave = base.copy(weight = newWeight, isFriendly = friendly, isNeutered = neutered)
            scope.launch {
                saving = true
                when (dogsVM.updateDogAndUser(toSave)) {
                    is Result.Success -> {
                        saving = false
                        editedDog = toSave
                        viewModel.invalidateUserCache()
                        viewModel.loadUserIfNeeded()
                        snackbar.showSnackbar("Dog updated")
                    }
                    is Result.Failure -> {
                        saving = false
                        snackbar.showSnackbar("Failed to update dog")
                    }
                }
            }
        },
        onDelete = { showDeleteConfirm = true }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = tab.icon,
                        label = { Text(tab.label) },
                        selected = (tab == selectedTab),
                        onClick = {
                            selectedTab = tab
                            if (tab == HomeTab.Yard) onGoYard()
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
                .padding(16.dp)
        ) {
            ProfileHeader(
                ownerName = user?.ownerName.orEmpty(),
                email = user?.email.orEmpty(),
                isLoading = user == null && errorText == null,
                errorText = errorText
            )

            Spacer(Modifier.height(16.dp))

            Text(text = "My Dogs (${dogs.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            DogsRow(
                dogs = dogs,
                selectedDogId = selectedDogId,
                onDogClick = { dog ->
                    selectedDogId = dog.id
                    editedDog = dog
                    weightField = dog.weight.toString()
                    friendly = dog.isFriendly
                    neutered = dog.isNeutered
                },
                onAddDog = onAddDog
            )

            Spacer(Modifier.height(20.dp))

            EditorSection(
                editorState = editorState,
                editorActions = editorActions
            )
        }
    }


    DeleteDogDialog(
        visible = showDeleteConfirm && editedDog != null,
        dogName = editedDog?.name.orEmpty(),
        onConfirm = {
            val id = editedDog!!.id
            scope.launch {
                when (dogsVM.deleteDogAndUser(id)) {
                    is Result.Success -> {
                        selectedDogId = null
                        editedDog = null
                        viewModel.invalidateUserCache()
                        viewModel.loadUserIfNeeded()
                        snackbar.showSnackbar("Dog deleted")
                    }
                    is Result.Failure -> snackbar.showSnackbar("Failed to delete dog")
                }
            }
            showDeleteConfirm = false
        },
        onDismiss = { showDeleteConfirm = false }
    )

    // Photo source chooser sheet (Camera / Gallery)
    if (showPhotoSourceSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            sheetState = sheetState
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Change photo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))


                if (hasCamera) {
                    ListItem(
                        headlineContent = { Text("Take photo") },
                        modifier = Modifier.clickable {
                            showPhotoSourceSheet = false
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("Choose from gallery") },
                    modifier = Modifier.clickable {
                        showPhotoSourceSheet = false
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }


    if (showOpenSettings) {
        AlertDialog(
            onDismissRequest = { showOpenSettings = false },
            title = { Text("Camera permission needed") },
            text = { Text("To take a photo, enable the Camera permission in App Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showOpenSettings = false
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showOpenSettings = false }) { Text("Cancel") }
            }
        )
    }

    BackHandler { onBack() }
}


@Composable
private fun ProfileHeader(
    ownerName: String,
    email: String,
    isLoading: Boolean,
    errorText: String?
) {
    Text(
        text = ownerName.ifBlank { "Owner name" },
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = email.ifBlank { "email@example.com" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    when {
        isLoading -> {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        !errorText.isNullOrBlank() -> {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DogsRow(
    dogs: List<DogDto>,
    selectedDogId: String?,
    onDogClick: (DogDto) -> Unit,
    onAddDog: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(dogs, key = { it.id }) { dog ->
            DogPhotoCard(
                dog = dog,
                isSelected = dog.id == selectedDogId,
                onClick = { onDogClick(dog) }
            )
        }
        item {
            AddDogTile(onClick = onAddDog)
        }
    }
}

@Composable
private fun EditorSection(
    editorState: DogEditorUiState?,
    editorActions: DogEditorActions
) {
    if (editorState == null) {
        Text(
            "Tap a dog above to edit its details.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    DogEditorCard(state = editorState, actions = editorActions)
}

data class DogEditorUiState(
    val dog: DogDto,
    val weightField: String,
    val friendly: Boolean,
    val neutered: Boolean,
    val uploading: Boolean,
    val saving: Boolean
)

data class DogEditorActions(
    val onWeightChange: (String) -> Unit,
    val onToggleFriendly: () -> Unit,
    val onToggleNeutered: () -> Unit,
    val onPickPhoto: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit
)

@Composable
private fun DogEditorCard(
    state: DogEditorUiState,
    actions: DogEditorActions
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit \"${state.dog.name.ifBlank { "Dog" }}\"", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(96.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (state.dog.photoUrl.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No photo", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        SubcomposeAsyncImage(
                            model = state.dog.photoUrl,
                            contentDescription = state.dog.name,
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = actions.onPickPhoto, enabled = !state.uploading) {
                    if (state.uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.uploading) "Uploading…" else "Change photo")
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = actions.onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            }

            OutlinedTextField(
                value = state.weightField,
                onValueChange = actions.onWeightChange,
                label = { Text("Weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Numbers only") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.friendly,
                    onClick = actions.onToggleFriendly,
                    label = { Text("Friendly") }
                )
                FilterChip(
                    selected = state.neutered,
                    onClick = actions.onToggleNeutered,
                    label = { Text("Neutered") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = actions.onSave, enabled = !state.saving) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.saving) "Saving…" else "Save changes")
                }
            }
        }
    }
}

@Composable
private fun DogPhotoCard(
    dog: DogDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = Modifier
            .size(width = 140.dp, height = 140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        val url = dog.photoUrl
        if (url.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photo", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = dog.name,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Failed", style = MaterialTheme.typography.bodySmall)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AddDogTile(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(width = 140.dp, height = 140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Dog",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DeleteDogDialog(
    visible: Boolean,
    dogName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete dog") },
        text = { Text("Are you sure you want to delete \"$dogName\"? This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
