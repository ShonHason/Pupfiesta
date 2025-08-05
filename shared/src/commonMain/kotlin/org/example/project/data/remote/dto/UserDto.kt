package org.example.project.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val email: String,
    val name: String,
    val dogList: List<DogDto>
)
