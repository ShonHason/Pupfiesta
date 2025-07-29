package org.example.project

data class Coordinates(val lat: Double, val lng: Double)

expect class LocationProvider() {

    suspend fun getCurrentLocation(): Coordinates
}
