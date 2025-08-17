package org.example.project.data.remote.dto

import androidx.compose.foundation.layout.Box
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.enum.Breed

@Serializable
data class DogDto(
    val id: String = "",
    val name: String = "",
    val breed: Breed = Breed.MIXED,
    val weight: Int = 0,
    @SerialName("dogPictureUrl")
    val dogPictureUrl: String = "",
    val isFriendly: Boolean = false,
    val isMale: Boolean = true,
    val isNeutered: Boolean = false,
    val ownerId: String = ""
) {
    val photoUrl: String get() = dogPictureUrl
}