package org.example.project.data.dogs
import org.example.project.enum.Breed
import org.example.project.data.remote.dto.DogDto

/**
 * Events for the DogsViewModel, analogous to UserEvent.
 */
sealed interface DogsEvent {
    data class IdChanged(val id: String) : DogsEvent
    data class NameChanged(val name: String) : DogsEvent
    data class BreedChanged(val breed: Breed?) : DogsEvent
    data class WeightChanged(val kg: Int) : DogsEvent
    data class ImageUrlChanged(val url: String?) : DogsEvent
    data class IsFriendlyChanged(val isFriendly: Boolean) : DogsEvent
    data class IsMaleChanged(val isMale: Boolean) : DogsEvent
    data class IsNeuteredChanged(val isNeutered: Boolean) : DogsEvent

    /** Load a dog dto into the form for editing */
    data class LoadFromDto(val dog: DogDto) : DogsEvent

    /** Reset to empty form */
    data object ResetState : DogsEvent

    /** Attempt to save current form (add or update by id) */
    data object OnSave : DogsEvent
}
