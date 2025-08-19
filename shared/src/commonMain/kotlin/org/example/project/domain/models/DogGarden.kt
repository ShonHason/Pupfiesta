package org.example.project.domain.models
import kotlinx.serialization.Serializable


@Serializable
data class DogGarden(
    val id: String,
    val name: String,
    val mapUrl: String,
    val location: Location,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val presentDogs:List<DogBrief> = emptyList()

    )


