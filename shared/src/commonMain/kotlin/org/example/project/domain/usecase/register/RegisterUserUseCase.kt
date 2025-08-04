package org.example.project.domain.usecase

import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.repository.AuthRepository

class RegisterUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String , dogList:List<DogDto>): Result<Unit, AuthError> {
        return authRepository.userRegistration(name, email, password, dogList)
    }
}