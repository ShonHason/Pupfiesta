package org.example.project.presentation.features.login

/**
 * Carries both form data (in Idle) and global load/success/error states
 */
public sealed class LoginState {
    data class Idle(
        val email: String = "",
        val password: String = ""
    ) : LoginState()

    object Loading : LoginState()
    data class Success(val userId: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
