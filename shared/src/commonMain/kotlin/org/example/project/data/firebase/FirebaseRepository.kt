package org.example.project.data.firebase

import kotlinx.coroutines.flow.Flow
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.DogGarden
import org.example.project.domain.models.User
import org.example.project.domain.models.dogError
import org.example.project.data.local.Result

interface FirebaseRepository {
    // User
    suspend fun userLogin(email: String, password: String): Result<Unit, AuthError>
    suspend fun userRegistration(email: String, password: String, name: String, dogs: List<DogDto>): Result<Unit, AuthError>
    suspend fun logout(): Result<Unit, AuthError>
    suspend fun updateUser(user: User): Result<Unit, AuthError>
    suspend fun getUserProfile(): Result<UserDto, AuthError>

    // Dog
    suspend fun addDogAndLinkToUser(dog: DogDto): Result<DogDto, dogError>
    suspend fun updateDogAndUser(dog: DogDto): Result<Unit, dogError>
    suspend fun deleteDogAndUser(dogId: String): Result<Unit, dogError>

    // Dog gardens
    suspend fun saveDogGardens(gardens: List<DogGarden>): Result<Unit, AuthError>
    suspend fun getDogGardens(): Result<List<DogGarden>, AuthError>
    suspend fun getDogGardensNear(latitude: Double, longitude: Double, radiusMeters: Int): Result<List<DogGarden>, AuthError>
    fun listenGardenPresence(gardenId: String): Flow<List<DogDto>>
    suspend fun checkInDogs(gardenId: String, dogs: List<DogDto>): Result<Unit, AuthError>
    suspend fun checkOutDogs(gardenId: String, dogIds: List<String>): Result<Unit, AuthError>
    suspend fun upsertDogGardensCore(items: List<DogGarden>):
          Result<Unit, AuthError>

}
