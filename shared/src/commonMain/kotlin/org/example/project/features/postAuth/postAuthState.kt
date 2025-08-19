package org.example.project.features.postAuth

sealed class PostAuthState {
    object Idle : PostAuthState()
    data class Running(val message: String) : PostAuthState()
    data class Success(val savedCount: Int) : PostAuthState()
    data class Error(val message: String) : PostAuthState()
}