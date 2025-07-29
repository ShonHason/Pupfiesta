package org.example.project


import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.*
import platform.Foundation.NSObject

/**
 * A tiny wrapper which turns CoreLocation callbacks into a suspend function.
 */
actual class LocationProvider {
    private val manager = CLLocationManager().apply {
        requestWhenInUseAuthorization()
    }

    actual suspend fun getCurrentLocation(): Coordinates =
        suspendCancellableCoroutine { cont ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    val locs = didUpdateLocations.filterIsInstance<CLLocation>()
                    if (locs.isNotEmpty()) {
                        val loc = locs.last()
                        cont.resume(Coordinates(loc.coordinate.latitude, loc.coordinate.longitude), null)
                        manager.stopUpdatingLocation()
                    }
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError
                ) {
                    cont.resumeWithException(Throwable(didFailWithError.localizedDescription))
                }
            }

            manager.delegate = delegate
            manager.startUpdatingLocation()

            cont.invokeOnCancellation {
                manager.stopUpdatingLocation()
            }
        }
}
