package org.example.project.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val ownerName: String = "",
    val dogList: List<DogDto> = emptyList()
)