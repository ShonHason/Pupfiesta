package org.example.project.models

data class User(
    val id: String,
    val ownerName: String,
    val dogList:List<Dog>
)


