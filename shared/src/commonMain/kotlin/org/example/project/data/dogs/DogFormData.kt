package org.example.project.data.dogs
import org.example.project.enum.Breed
import org.example.project.enum.Gender

/**
 * Editable dog form (like UserFormData).
 * Keep breed nullable so you can enforce selection on Save.
 */
data class DogFormData(
    val id: String = "",
    val name: String = "",
    val breed: Breed? = null,
    val weight: Int = 10,
    val imgUrl: String? = null,
    val isFriendly: Boolean = true,
    val isMale: Boolean = true,      // you can also keep a Gender if you prefer
    val isNeutered: Boolean = false,
    val ownerId: String = "",
    val errors: List<String> = emptyList()
)
