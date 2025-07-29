package org.example.project

actual class LocationProvider actual constructor() {
    /**
     * Suspends until we have a location or fail.
     * On iOS this will wrap CoreLocation; on Android FusedLocation.
     */
    actual suspend fun getCurrentLocation(): Coordinates {
        TODO("Not yet implemented")
    }
}