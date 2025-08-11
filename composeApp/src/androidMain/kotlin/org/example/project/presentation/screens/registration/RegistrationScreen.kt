// file: RegistrationScreen.kt
package org.example.project.presentation.screens.registration

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    // Observe registration state
    val registrationState by viewModel.userState.collectAsState()

    // Local UI state
    var selectedBreed by remember { mutableStateOf(Breed.MIXED) }
    var breedMenuExpanded by remember { mutableStateOf(false) }
    var isMale by remember { mutableStateOf(true) }
    var isNeutered by remember { mutableStateOf(true) }
    var isFriendly by remember { mutableStateOf(true) }
    var selectedWeight by remember { mutableStateOf(25) }
    var dogImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> dogImageUri = uri }

    // Navigate on success
    if (registrationState is UserState.Loaded) {
        LaunchedEffect(Unit) { onRegistered() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient
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

        // Scrollable form content, padded below back arrow
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

            // Email
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

            // Password
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

            // Owner’s Name
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

            // Dog’s Name
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

            // Breed selector
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
                            .replace('_',' ')
                            .split(' ')
                            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                        modifier = Modifier
                            .clickable { breedMenuExpanded = true }
                            .background(Color.Gray.copy(alpha=0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                    DropdownMenu(
                        expanded = breedMenuExpanded,
                        onDismissRequest = { breedMenuExpanded = false }
                    ) {
                        Breed.values().forEach { breed ->
                            DropdownMenuItem(
                                text = { Text(
                                    breed.name
                                        .lowercase()
                                        .replace('_',' ')
                                        .split(' ')
                                        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                                )},
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

            // Gender toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal=16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        isMale = !isMale
                        viewModel.onEvent(UserEvent.DogGenderChanged(if(isMale) Gender.MALE else Gender.FEMALE))
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isMale) Color(0xFFD0E9FF) else Color(0xFFFFD0E0),
                        contentColor = if(isMale) Color(0xFF1196F3) else Color(0xFFFF4081)
                    ),
                    modifier = Modifier.size(80.dp,32.dp)
                ) { Text(if(isMale) "Male" else "Female") }
            }

            Spacer(Modifier.height(16.dp))

            // Neutered & Friendly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal=16.dp),
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
                Text(if(isNeutered) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal=16.dp),
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
                Text(if(isFriendly) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))

            // Weight & Photo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal=16.dp),
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
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        if (dogImageUri != null) {
                            AsyncImage(
                                model = dogImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sign Up
            Button(
                onClick = { viewModel.onEvent(UserEvent.OnSignUp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal=16.dp, vertical=16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Up")
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
            Icon(Icons.Filled.ArrowBack, contentDescription="Back")
        }
    }
}
