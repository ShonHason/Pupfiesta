// file: RegistrationScreen.kt
package org.example.project.presentation.screens.registration

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.launch
import org.example.project.CloudinaryUploader
import org.example.project.data.user.UserEvent
import org.example.project.data.user.UserFormData
import org.example.project.data.user.UserState
import org.example.project.enum.Breed
import org.example.project.enum.Gender
import org.example.project.features.registration.UserViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val registrationState by viewModel.userState.collectAsState()

    var selectedBreed by remember { mutableStateOf(Breed.MIXED) }
    var breedMenuExpanded by remember { mutableStateOf(false) }
    var isMale by remember { mutableStateOf(true) }
    var isNeutered by remember { mutableStateOf(true) }
    var isFriendly by remember { mutableStateOf(true) }
    var selectedWeight by remember { mutableStateOf(25) }
    var dogImageUri by remember { mutableStateOf<Uri?>(null) }
    var dogImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dogImageUrl by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var signingUp by remember { mutableStateOf(false) }
    var signUpError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    if (registrationState is UserState.Loaded) {
        LaunchedEffect(Unit) { onRegistered() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F1C4),
                            Color(0xFFB0D4F8)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 56.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Let’s Get To Know You!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            val data = (registrationState as? UserState.Initial)?.data ?: UserFormData()

            OutlinedTextField(
                value = data.email,
                onValueChange = { viewModel.onEvent(UserEvent.EmailChanged(it)) },
                placeholder = { Text("Enter Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = data.password,
                onValueChange = { viewModel.onEvent(UserEvent.PasswordChanged(it)) },
                placeholder = { Text("Enter Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = data.ownerName,
                onValueChange = { viewModel.onEvent(UserEvent.OwnerNameChanged(it)) },
                placeholder = { Text("Enter Owner’s Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = data.dogName,
                onValueChange = { viewModel.onEvent(UserEvent.DogNameChanged(it)) },
                placeholder = { Text("Enter Dog’s Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("What is your dog’s breed?", style = MaterialTheme.typography.bodyMedium)
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
                        Breed.values().forEach { breed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        breed.name
                                            .lowercase()
                                            .replace('_', ' ')
                                            .split(' ')
                                            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                                    )
                                },
                                onClick = {
                                    selectedBreed = breed
                                    viewModel.onEvent(UserEvent.DogBreedChanged(breed))
                                    breedMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        isMale = !isMale
                        viewModel.onEvent(
                            UserEvent.DogGenderChanged(
                                if (isMale) Gender.MALE else Gender.FEMALE
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMale) Color(0xFFD0E9FF) else Color(0xFFFFD0E0),
                        contentColor = if (isMale) Color(0xFF1196F3) else Color(0xFFFF4081)
                    ),
                    modifier = Modifier.size(80.dp, 32.dp)
                ) { Text(if (isMale) "Male" else "Female") }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Neutered/Spayed?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isNeutered,
                    onCheckedChange = {
                        isNeutered = it
                        viewModel.onEvent(UserEvent.IsNeuteredChanged(it))
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isNeutered) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Friendly?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isFriendly,
                    onCheckedChange = {
                        isFriendly = it
                        viewModel.onEvent(UserEvent.IsFriendlyChanged(it))
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isFriendly) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Dog Weight (kg)", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = selectedWeight.toFloat(),
                        onValueChange = {
                            selectedWeight = it.roundToInt()
                            viewModel.onEvent(UserEvent.DogWeightChanged(selectedWeight))
                        },
                        valueRange = 5f..50f,
                        modifier = Modifier.width(120.dp)
                    )
                    Text("${selectedWeight}kg", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tap to upload picture", style = MaterialTheme.typography.bodyMedium)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    ) {
                        IconButton(onClick = { showImageSourceDialog = true }) {
                            when {
                                dogImageBitmap != null -> {
                                    Image(
                                        bitmap = dogImageBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                dogImageUri != null -> {
                                    AsyncImage(
                                        model = dogImageUri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
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
                        if (!uploading && dogImageUrl != null) {
                            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                                Text("✓", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    if (uploadError != null) {
                        Text(uploadError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (uploading) { signUpError = "Please wait for the photo upload to finish"; return@Button }
                    val email = data.email.trim()
                    val password = data.password
                    if (email.isBlank() || password.isBlank()) { signUpError = "Email and password are required"; return@Button }
                    if (dogImageUrl.isNullOrBlank()) { signUpError = "Please add a dog photo first"; return@Button }

                    signingUp = true
                    signUpError = null
                    scope.launch {
                        try {
                            val result = Firebase.auth.createUserWithEmailAndPassword(email, password)
                            val uid = result.user?.uid ?: Firebase.auth.currentUser?.uid ?: ""
                            if (uid.isBlank()) error("No uid")

                            val dog = org.example.project.domain.models.Dog(
                                id = uid,
                                dogName = data.dogName,
                                dogBreed = selectedBreed,
                                dogGender = if (isMale) org.example.project.enum.Gender.MALE else org.example.project.enum.Gender.FEMALE,
                                dogPictureUrl = dogImageUrl,
                                isNeutered = isNeutered,
                                dogWeight = selectedWeight,
                                isFriendly = isFriendly
                            )

                            val user = org.example.project.domain.models.User(
                                id = uid,
                                email = email,
                                ownerName = data.ownerName,
                                dogList = listOf(dog)
                            )

                            Firebase.firestore.collection("Dogs").document(uid).set(dog, encodeDefaults = true)
                            Firebase.firestore.collection("Users").document(uid).set(user, encodeDefaults = true)

                            onRegistered()
                        } catch (t: Throwable) {
                            signUpError = t.message ?: "Sign up failed"
                        } finally {
                            signingUp = false
                        }
                    }
                },
                enabled = !uploading && !signingUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (signingUp) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Signing Up…")
                } else {
                    Text("Sign Up")
                }
            }

            if (signUpError != null) {
                Text(
                    signUpError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(36.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
}
