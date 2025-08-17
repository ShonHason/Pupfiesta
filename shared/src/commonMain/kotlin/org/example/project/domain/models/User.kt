package org.example.project.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val ownerName: String,
    val dogList: List<Dog>
)


