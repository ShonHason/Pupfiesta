package org.example.project.domain.repository

import org.example.project.data.local.Error
import org.example.project.data.local.Result
import org.example.project.domain.models.Dog


interface DogRepository{

    suspend fun getOwnerDogs(): Result<List<Dog>, Error>
    suspend fun getDogById(dogId: String): Result<Dog, Error>





}