// file: RegistrationScreen.kt
package org.example.project.presentation.screens.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.example.project.data.user.UserEvent
import org.example.project.data.user.UserFormData
import org.example.project.data.user.UserState
import kotlin.math.roundToInt
import org.example.project.enum.Breed
import org.example.project.enum.Gender
import org.example.project.features.registration.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: UserViewModel,
        onBack: () -> Unit,
        onRegistered: () -> Unit
) {
    // 1️⃣ Observe the registration state from the ViewModel
    val registrationState by viewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("User Registration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (registrationState) {
                is UserState.Initial -> {
                    // Idle state → show the form
                    val data = (registrationState as UserState.Initial).data
                    RegistrationForm(
                        formData = data,
                        onEvent = viewModel::onEvent
                    )
                }
                UserState.Loading -> {
                    // Loading state → centered spinner
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                UserState.Loaded -> {
                    // Loaded state → navigate away
                    LaunchedEffect(Unit) { onRegistered() }
                }
                is UserState.Error -> {
                    // Error state → show error message + retry
                    val msg = (registrationState as UserState.Error).message
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(UserEvent.ResetState) }) {
                            Text("Retry")
                        }
                    }
                    // Re-show the form beneath the error
                    val data = (viewModel.userState.value as? UserState.Initial)?.data
                        ?: UserFormData()
                    RegistrationForm(
                        formData = data,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationForm(
    formData: UserFormData,
    onEvent: (UserEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Email
        OutlinedTextField(
            value = formData.email,
            onValueChange = { onEvent(UserEvent.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        // Password
        OutlinedTextField(
            value = formData.password,
            onValueChange = { onEvent(UserEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Owner Name
        OutlinedTextField(
            value = formData.ownerName,
            onValueChange = { onEvent(UserEvent.OwnerNameChanged(it)) },
            label = { Text("Owner Name") },
            singleLine = true
        )

        // Dog Name
        OutlinedTextField(
            value = formData.dogName,
            onValueChange = { onEvent(UserEvent.DogNameChanged(it)) },
            label = { Text("Dog Name") },
            singleLine = true
        )

        // Breed Selector
        BreedSelector(
            selectedBreed = formData.dogBreed,
            onBreedSelected = { onEvent(UserEvent.DogBreedChanged(it)) }
        )

        // Gender Toggle (blue for Male, pink for Female)
        GenderToggle(
            selectedGender = formData.dogGender,
            onGenderSelected = { onEvent(UserEvent.DogGenderChanged(it)) }
        )

        // Neutered & Friendly switches
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Neutered")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = formData.isNeutered,
                onCheckedChange = { onEvent(UserEvent.IsNeuteredChanged(it)) }
            )
            Spacer(Modifier.width(24.dp))
            Text("Friendly")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = formData.isFriendly,
                onCheckedChange = { onEvent(UserEvent.IsFriendlyChanged(it)) }
            )
        }

        // Weight Slider
        Column {
            Text("Weight: ${formData.dogWeight} kg")
            Slider(
                value = formData.dogWeight.toFloat(),
                onValueChange = { onEvent(UserEvent.DogWeightChanged(it.roundToInt())) },
                valueRange = 1f..100f
            )
        }

        // Submit Button
        Button(
            onClick = { onEvent(UserEvent.OnSignUp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Changes")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreedSelector(
    selectedBreed: Breed,
    onBreedSelected: (Breed) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val breeds = Breed.values().toList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedBreed.name.lowercase()
                .replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Breed") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            breeds.forEach { breed ->
                DropdownMenuItem(
                    text = {
                        Text(
                            breed.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    },
                    onClick = {
                        onBreedSelected(breed)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GenderToggle(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onGenderSelected(Gender.MALE) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedGender == Gender.MALE)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("Male")
        }
        Button(
            onClick = { onGenderSelected(Gender.FEMALE) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedGender == Gender.FEMALE)
                    Color(0xFFFFC0CB)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("Female")
        }
    }
}
