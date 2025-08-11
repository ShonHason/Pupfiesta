// shared/src/iosMain/kotlin/org/example/project/utils/LocationUtils.ios.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.example.project.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual suspend fun getLocation(): Location = withContext(Dispatchers.Main.immediate) {
    suspendCancellableCoroutine { cont ->
        val manager = CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyHundredMeters
        }

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

            // iOS 14+
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                when (manager.authorizationStatus) {
                    kCLAuthorizationStatusAuthorizedWhenInUse,
                    kCLAuthorizationStatusAuthorizedAlways -> manager.startUpdatingLocation()
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted ->
                        fail("Location permission denied")
                    kCLAuthorizationStatusNotDetermined ->
                        manager.requestWhenInUseAuthorization()
                    else -> Unit
                }
            }

            // iOS 13 and earlier
            override fun locationManager(
                manager: CLLocationManager,
                didChangeAuthorizationStatus: CLAuthorizationStatus
            ) {
                when (didChangeAuthorizationStatus) {
                    kCLAuthorizationStatusAuthorizedWhenInUse,
                    kCLAuthorizationStatusAuthorizedAlways -> manager.startUpdatingLocation()
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted ->
                        fail("Location permission denied")
                    kCLAuthorizationStatusNotDetermined ->
                        manager.requestWhenInUseAuthorization()
                    else -> Unit
                }
            }

            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>
            ) {
                val last = didUpdateLocations.lastOrNull() as? CLLocation
                    ?: return fail("Location unavailable")

                // CLLocationCoordinate2D is a C struct: read via useContents
                last.coordinate.useContents {
                    succeed(Location(latitude = latitude, longitude = longitude))
                }
            }

            override fun locationManager(
                manager: CLLocationManager,
                didFailWithError: NSError
            ) {
                fail(didFailWithError.localizedDescription ?: "Location unavailable")
            }

            private fun succeed(value: Location) {
                if (cont.isActive) cont.resume(value)
                cleanup()
            }

            private fun fail(msg: String) {
                if (cont.isActive) cont.resumeWithException(IllegalStateException(msg))
                cleanup()
            }

            private fun cleanup() {
                manager.stopUpdatingLocation()
                manager.delegate = null
            }
        }

        manager.delegate = delegate

        // Kick off auth / updates
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> manager.startUpdatingLocation()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted ->
                cont.resumeWithException(IllegalStateException("Location permission denied"))
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            else -> manager.requestWhenInUseAuthorization()
        }

        cont.invokeOnCancellation {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }
}
