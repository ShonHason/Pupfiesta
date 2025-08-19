package org.example.project.presentation.screens.addDog

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import org.example.project.CloudinaryUploader
import org.example.project.enum.Breed
import org.example.project.enum.Gender
import org.example.project.features.registration.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDogScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    var dogName by remember { mutableStateOf("") }
    var selectedBreed by remember { mutableStateOf(Breed.MIXED) }
    var breedMenuExpanded by remember { mutableStateOf(false) }
    var isMale by remember { mutableStateOf(true) }
    var isNeutered by remember { mutableStateOf(true) }
    var isFriendly by remember { mutableStateOf(true) }
    var weight by remember { mutableStateOf(25) }

    // image picking / taking
    var dogImageUri by remember { mutableStateOf<Uri?>(null) }
    var dogImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dogImageUrl by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // save state
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            dogImageBitmap = null
            dogImageUri = uri
            uploadError = null
            uploading = true
            CloudinaryUploader.upload(context, uri) { url ->
                uploading = false
                dogImageUrl = url
                if (url == null) uploadError = "Upload failed"
            }
        }
    }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            dogImageUri = null
            dogImageBitmap = bitmap
            uploadError = null
            uploading = true
            CloudinaryUploader.upload(context, bitmap) { url ->
                uploading = false
                dogImageUrl = url
                if (url == null) uploadError = "Upload failed"
            }
        }
    }

    // ---- UI ----
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Dog") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = dogName,
                onValueChange = { dogName = it },
                label = { Text("Dog’s Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Breed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Breed", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Box {
                    Text(
                        text = selectedBreed.name
                            .lowercase()
                            .replace('_', ' ')
                            .split(' ')
                            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                        modifier = Modifier
                            .clickable { breedMenuExpanded = true }
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                    DropdownMenu(
                        expanded = breedMenuExpanded,
                        onDismissRequest = { breedMenuExpanded = false }
                    ) {
                        Breed.values().forEach { b ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        b.name.lowercase()
                                            .replace('_', ' ')
                                            .split(' ')
                                            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                                    )
                                },
                                onClick = {
                                    selectedBreed = b
                                    breedMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Gender
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                val maleLabel = if (isMale) "Male" else "Female"
                Button(
                    onClick = { isMale = !isMale },
                    shape = RoundedCornerShape(16.dp)
                ) { Text(maleLabel) }
            }

            // Neutered
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Neutered/Spayed?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = isNeutered, onCheckedChange = { isNeutered = it })
            }

            // Friendly
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Friendly?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = isFriendly, onCheckedChange = { isFriendly = it })
            }

            // Weight
            Column {
                Text("Dog Weight (kg)", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = weight.toFloat(),
                    onValueChange = { weight = it.toInt() },
                    valueRange = 5f..50f,
                    modifier = Modifier.width(180.dp)
                )
                Text("$weight kg", style = MaterialTheme.typography.bodyMedium)
            }

            // Photo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tap to upload picture", style = MaterialTheme.typography.bodyMedium)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                ) {
                    IconButton(onClick = { showImageSourceDialog = true }) {
                        when {
                            dogImageBitmap != null -> {
                                Image(
                                    bitmap = dogImageBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            dogImageUri != null -> {
                                SubcomposeAsyncImage(
                                    model = dogImageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape),
                                    loading = { CircularProgressIndicator(strokeWidth = 2.dp) }
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                                )
                            }
                        }
                    }
                    if (uploading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
                if (uploadError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(uploadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (!uploading && !dogImageUrl.isNullOrBlank()) {
                    Text("Photo uploaded ✓", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                }
            }


            Button(
                onClick = {
                    if (uploading) { saveError = "Please wait for the photo upload to finish"; return@Button }
                    if (dogName.isBlank()) { saveError = "Dog name is required"; return@Button }
                    val url = dogImageUrl
                    if (url.isNullOrBlank()) { saveError = "Please add a dog photo"; return@Button }

                    saveError = null
                    saving = true
                    scope.launch {
                        val res = viewModel.addDog(
                            name = dogName,
                            breed = selectedBreed,
                            weight = weight,
                            isFriendly = isFriendly,
                            isMale = isMale,
                            isNeutered = isNeutered,
                            pictureUrl = url
                        )
                        saving = false
                        when (res) {
                            is org.example.project.data.local.Result.Success -> {viewModel.refreshUser()
                                onDone()}
                            is org.example.project.data.local.Result.Failure -> {
                                saveError = res.error?.message ?: "Failed to add dog"
                            }
                        }
                    }
                },
                enabled = !saving && !uploading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Saving…")
                } else {
                    Text("Save Dog")
                }
            }

            if (saveError != null) {
                Text(saveError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }


    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Upload photo") },
            text = { Text("Choose how you'd like to provide the photo") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    takePicturePreviewLauncher.launch(null)
                }) { Text("Take photo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }) { Text("Choose from library") }
            }
        )
    }
}
