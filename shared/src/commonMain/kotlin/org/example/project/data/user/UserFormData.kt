package org.example.project.data.user

import org.example.project.enum.Breed
import org.example.project.enum.Gender

data class UserFormData(
    val email: String = "",
    val password: String = "",
    val ownerName: String = "",
    val dogBreed: Breed = Breed.MIXED,
    val dogName: String = "",
    val dogGender: Gender =  Gender.MALE,
    val dogPictureUrl: String? = null,
    val dogWeight: Int = 10,
    val isNeutered: Boolean = false,
    val isFriendly: Boolean = false,
    val errors: List<String> = emptyList()
)