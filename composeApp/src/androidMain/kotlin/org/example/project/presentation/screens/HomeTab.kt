package org.example.project.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon

enum class HomeTab(val label: String, val icon: @Composable () -> Unit) {
    Yard("Yard", { Icon(Icons.Filled.Pets, contentDescription = "Yard") }),
    Profile("Profile", { Icon(Icons.Filled.Person, contentDescription = "Profile") })
}