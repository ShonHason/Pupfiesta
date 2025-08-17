package org.example.project.data.user
import org.example.project.enum.Breed
import org.example.project.enum.Gender

sealed interface UserEvent{
    data class EmailChanged(val email: String): UserEvent
    data class PasswordChanged(val password: String): UserEvent
    data class OwnerNameChanged(val ownerName: String): UserEvent
    data class DogNameChanged(val dogName: String): UserEvent
    data class DogBreedChanged(val dogBreed: Breed): UserEvent
    data class DogGenderChanged(val dogGender: Gender): UserEvent
    data class IsFriendlyChanged(val isFriendly: Boolean): UserEvent
    data class IsNeuteredChanged(val isNeutered: Boolean): UserEvent
    data class DogWeightChanged(val dogWeight: Int): UserEvent
    data class DogPictureUrlChanged(val url: String?): UserEvent

    data object ResetState : UserEvent
    data object OnSignUp : UserEvent
    data object OnSignIn : UserEvent
}