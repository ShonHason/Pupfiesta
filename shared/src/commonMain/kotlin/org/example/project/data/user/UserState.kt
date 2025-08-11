package org.example.project.data.user

sealed class UserState {
    data class Initial(val data: UserFormData = UserFormData()) : UserState()
    data object Loading : UserState()
    data object Loaded : UserState()
    data class Error(val message: String) : UserState()
}
