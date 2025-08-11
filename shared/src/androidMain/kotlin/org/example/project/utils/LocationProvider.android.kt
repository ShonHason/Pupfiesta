// androidMain/src/main/kotlin/org/example/project/utils/LocationUtils.android.kt
package org.example.project.utils

import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import org.example.project.MyLocationApp

@SuppressLint("MissingPermission") // assume permissions are already granted
actual suspend fun getLocation(): Location {
    val client = LocationServices
        .getFusedLocationProviderClient(MyLocationApp.ctx)

    val last = client.lastLocation.await()
        ?: throw IllegalStateException("Location unavailable")

    return Location(
        latitude  = last.latitude,
        longitude = last.longitude
    )
}
