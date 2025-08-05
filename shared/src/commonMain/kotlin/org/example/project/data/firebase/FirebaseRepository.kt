package org.example.project.data.firebase

import dev.gitlive.firebase.auth.FirebaseUser
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.User

interface FirebaseRepository {
    suspend fun userLogin(email: String, password: String): Result<Unit, AuthError>
    suspend fun userRegistration(email: String, password: String, name: String, dogs: List<DogDto>): Result<Unit, AuthError>

    suspend fun getCurrentUser(): Result<FirebaseUser, AuthError>

    suspend fun updateUser(user: User): Result<Unit, AuthError>

    suspend fun logout(): Result<Unit, AuthError>
}