// shared/src/commonMain/kotlin/org/example/project/data/firebase/FirebaseRepository.kt
package org.example.project.data.firebase

import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.data.user.UserFormData
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.DogGarden
import org.example.project.domain.models.User

interface FirebaseRepository {
    // User
    suspend fun userLogin(email: String, password: String): Result<Unit, AuthError>
    suspend fun userRegistration(email: String, password: String, name: String, dogs: List<DogDto>): Result<Unit, AuthError>
    suspend fun updateUser(user: User): Result<Unit, AuthError>
    suspend fun logout(): Result<Unit, AuthError>
    suspend fun getUserProfile(): Result<UserDto, AuthError>

    // Dog gardens (restored)
    suspend fun saveDogGardens(gardens: List<DogGarden>): Result<Unit, AuthError>
    suspend fun getDogGardens(): Result<List<DogGarden>, AuthError>
    suspend fun getDogGardensNear(latitude: Double, longitude: Double, radiusMeters: Int): Result<List<DogGarden>, AuthError>
}
