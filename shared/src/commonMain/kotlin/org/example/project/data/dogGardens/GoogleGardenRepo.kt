// shared/src/commonMain/kotlin/org/example/project/data/dogGardens/GoogleGardensRepo.kt
package org.example.project.data.dogGardens

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.domain.models.DogGarden
import org.example.project.domain.models.Location

class GoogleGardensRepo(
    private val client: HttpClient,
    private val apiKey: String
) : GardensRepository {

    companion object {
        private const val BASE = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    }

    override suspend fun searchDogParks(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        language: String
    ): List<DogGarden> {
        val http = client.config {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }

        val resp: NearbySearchResponse = http.get(BASE) {
            parameter("location", "$latitude,$longitude")
            parameter("radius", radiusMeters)
            parameter("type", "park")
            parameter("keyword", "dog park|גינת כלבים")
            parameter("language", language)
            parameter("key", apiKey)
        }.body()

        return resp.results.mapNotNull { r ->
            val lat = r.geometry?.location?.lat ?: return@mapNotNull null
            val lng = r.geometry?.location?.lng ?: return@mapNotNull null
            val placeId = r.place_id ?: "$lat,$lng"

            DogGarden(
                id = placeId,
                name = r.name ?: "Dog Park",
                mapUrl = "https://www.google.com/maps/place/?q=place_id:$placeId",
                location = Location(latitude = lat, longitude = lng)
            )
        }
    }
}

@Serializable
private data class NearbySearchResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String? = null,
    val next_page_token: String? = null
)

@Serializable
private data class PlaceResult(
    val place_id: String? = null,
    val name: String? = null,
    val vicinity: String? = null,
    val formatted_address: String? = null,
    val geometry: Geometry? = null
)

@Serializable
private data class Geometry(val location: LatLngDto? = null)

// Make these nullable so bad/missing items don't crash deserialization
@Serializable
private data class LatLngDto(val lat: Double? = null, val lng: Double? = null)
