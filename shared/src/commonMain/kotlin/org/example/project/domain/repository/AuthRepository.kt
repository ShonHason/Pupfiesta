package org.example.project.data.firebase

interface AuthRepository {
    suspend fun userLogin(email: String, password: String): Result<Unit>
    suspend fun userRegistration(email: String, password: String, name: String, dogList: List<String>): Result<Unit>
}