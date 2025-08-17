package org.example.project.presentation.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import org.example.project.features.registration.UserViewModel
import org.example.project.presentation.screens.HomeTab // or org.example.project.ui.HomeTab
import org.example.project.data.remote.dto.UserDto
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.local.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onGoYard: () -> Unit,
    onAddDog: () -> Unit
) {
    var user by remember { mutableStateOf<UserDto?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Load ONCE via VM cache. If already loaded, this is a no-op (no network).
    LaunchedEffect(Unit) {
        when (val r = viewModel.loadUserIfNeeded()) {
            is Result.Success -> errorText = null
            is Result.Failure -> errorText = r.error?.message ?: "Failed to load profile"
        }
    }

    // Observe the cached user from the VM and reflect it locally for the UI
    val cachedUser by viewModel.currentUser.collectAsState()
    LaunchedEffect(cachedUser) { user = cachedUser }

    val ownerName = user?.ownerName.orEmpty()
    val email = user?.email.orEmpty()
    val dogs: List<DogDto> = user?.dogList ?: emptyList()

    var selectedTab by remember { mutableStateOf(HomeTab.Profile) }

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
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
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
                user == null && errorText == null -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                errorText != null -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(text = "My Dogs (${dogs.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Always show the row with an extra "+" tile at the end
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dogs, key = { it.id }) { dog ->
                    DogPhotoCard(dog = dog)
                }
                item {
                    AddDogTile(onClick = onAddDog)
                }
            }
        }
    }

    BackHandler { onBack() }
}

@Composable
private fun DogPhotoCard(dog: DogDto) {
    Card(
        modifier = Modifier.size(width = 140.dp, height = 140.dp),
        shape = RoundedCornerShape(16.dp)
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
