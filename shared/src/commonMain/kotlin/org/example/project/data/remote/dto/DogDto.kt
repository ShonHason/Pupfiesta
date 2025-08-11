package org.example.project.data.remote.dto

import kotlinx.serialization.Serializable
import org.example.project.enum.Breed
import org.example.project.enum.Gender

@Serializable
data class DogDto(
    val id: String = "",
    val name: String,
    val breed: Breed,
    val weight: Int,
    val imgUrl: String,
    val isFriendly: Boolean,
    val isMale: Boolean,
    val isNeutered: Boolean,
    val ownerId: String = "",
)
