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

class GoogleGardensRepository(
    private val client: HttpClient,
    private val apiKey: String
) : GardensRepository {

    companion object {
        private const val BASE = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        private const val DETAILS_BASE = "https://maps.googleapis.com/maps/api/place/details/json"
        private const val PHOTO_BASE = "https://maps.googleapis.com/maps/api/place/photo"
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

        // Google Nearby Search: rankby=distance requires NOT using radius; provide type/keyword.
        val resp: NearbySearchResponse = http.get(BASE) {
            parameter("location", "$latitude,$longitude")
            parameter("rankby", "distance")
            parameter("type", "park")
            parameter("keyword", "גינת כלבים")
            parameter("language", language)
            parameter("key", apiKey)
        }.body()

        // Map API results to domain model
        val raw = resp.results.mapNotNull { r ->
            val lat = r.geometry?.location?.lat ?: return@mapNotNull null
            val lng = r.geometry?.location?.lng ?: return@mapNotNull null
            val placeId = r.place_id ?: return@mapNotNull null
            val name = r.name?.trim() ?: "גינת כלבים"

            DogGarden(
                id = placeId,
                name = name,
                mapUrl = "https://www.google.com/maps/place/?q=place_id:$placeId",
                location = org.example.project.domain.models.Location(lat, lng)
            )
        }

        // Client-side distance filter so changing the radius in the UI updates the map
        val maxDist = radiusMeters.coerceAtLeast(1)
        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
            val R = 6_371_000.0
            fun Double.rad() = this * kotlin.math.PI / 180.0
            val dLat = (lat2 - lat1).rad()
            val dLon = (lon2 - lon1).rad()
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                    kotlin.math.cos(lat1.rad()) * kotlin.math.cos(lat2.rad()) *
                    kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return (R * c).toInt()
        }

        return raw.filter { g ->
            haversineMeters(latitude, longitude, g.location.latitude, g.location.longitude) <= maxDist
        }
    }

    override suspend fun getPlacePhotoUrl(
        placeId: String,
        maxWidth: Int
    ): String? {
        val http = client.config {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }

        // 1) Ask for photo references for this place
        val details: PlaceDetailsResponse = http.get(DETAILS_BASE) {
            parameter("place_id", placeId)
            parameter("fields", "photos")
            parameter("key", apiKey)
        }.body()

        val ref = details.result?.photos?.firstOrNull()?.photo_reference ?: return null

        // 2) Return the resolvable PHOTO endpoint URL (Coil will follow the redirect)
        val width = maxWidth.coerceIn(100, 1600) // safe bounds per API docs
        return "$PHOTO_BASE?maxwidth=$width&photo_reference=$ref&key=$apiKey"
    }

    // -------- DTOs --------

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

    @Serializable
    private data class LatLngDto(val lat: Double? = null, val lng: Double? = null)

    @Serializable
    private data class PlaceDetailsResponse(
        val result: PlaceDetailsResult? = null,
        val status: String? = null
    )

    @Serializable
    private data class PlaceDetailsResult(
        val photos: List<PlacePhotoRef>? = null
    )

    @Serializable
    private data class PlacePhotoRef(
        val photo_reference: String? = null
    )
}
