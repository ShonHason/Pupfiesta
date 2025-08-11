// domain/models/Location.kt
package org.example.project.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
