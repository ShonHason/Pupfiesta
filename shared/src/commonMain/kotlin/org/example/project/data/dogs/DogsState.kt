package org.example.project.data.dogs

/**
 * Mirrors your UserState:
 * - Initial carries DogFormData (like UserFormData)
 * - Loading / Loaded / Error for banners
 */
sealed class DogsState {
    data class Initial(val data: DogFormData = DogFormData()) : DogsState()
    data object Loading : DogsState()
    data object Loaded : DogsState()
    data class Error(val message: String) : DogsState()
}
