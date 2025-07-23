package org.example.project.domain.modelsEntities

data class User(
    val id: String,
    val ownerName: String,
    val dogList:List<Dog>

)


