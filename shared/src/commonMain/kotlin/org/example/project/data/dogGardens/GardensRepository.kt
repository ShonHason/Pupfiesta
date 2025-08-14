package org.example.project.data.dogGardens
import org.example.project.domain.models.DogGarden

interface GardensRepository {
    suspend fun searchDogParks(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        language: String = "he"
    ): List<DogGarden>
}
