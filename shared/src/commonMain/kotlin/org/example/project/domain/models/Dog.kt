package org.example.project.domain.models


import kotlinx.serialization.Serializable
import org.example.project.enum.Breed
import org.example.project.enum.Gender

@Serializable
data class Dog(
    val id: String = "",
    val dogName: String,
    val dogBreed: Breed,
    val dogGender: Gender,
    val dogPictureUrl: String? = null,
    val isNeutered: Boolean,
    val dogWeight: Int,
    val isFriendly: Boolean,
)