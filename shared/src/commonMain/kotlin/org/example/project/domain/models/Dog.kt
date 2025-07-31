package org.example.project.models


import org.example.project.enum.Breed
import org.example.project.enum.Gender

data class Dog(
    val dogName: String,
    val dogBreed: Breed,
    val dogGender: Gender,
    val dogPictureUrl: String? = null,
    val isNeutered: Boolean, //
    val dogWeight: Int, // in kg
    val isFriendly: Boolean, // true if the dog is friendly with other dogs
)
