package org.example.project.data.repository.dto

import kotlinx.serialization.Serializable
import org.example.project.enum.Breed

@Serializable
data class DogDto(
    val id: String = "",
    val name: String,
    val breed: Breed,
    val weight: Double,
    val imgUrl: String,
    val isFriendly: Boolean,
    val isMale: Boolean,
    val isNeutered: Boolean,
    val ownerId: String = ""
)
