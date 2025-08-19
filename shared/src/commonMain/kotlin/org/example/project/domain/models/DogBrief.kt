package org.example.project.domain.models

import kotlinx.serialization.Serializable
import org.example.project.enum.Gender

@Serializable
data class DogBrief(
    val id: String,
    val dogName: String = "",
    val dogGender: Gender? = null,
    val isFriendly: Boolean? = null,
    val isNeutered: Boolean? = null,
    val dogPictureUrl: String = ""
)