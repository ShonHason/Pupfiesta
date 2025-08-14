package org.example.project.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

lateinit var appContext: Context

private const val TAG = "GetLocation"

@RequiresPermission(anyOf = [
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
])
actual suspend fun getLocation(): Location {
    val ctx = appContext

    val hasFine   = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        Log.w(TAG, "No location permission")
        throw SecurityException("Location permission not granted")
    }
    Log.d(TAG, "perm fine=$hasFine coarse=$hasCoarse")
    Log.d(TAG, "providersEnabled=${isLocationEnabled(ctx)}")

    if (!isLocationEnabled(ctx)) {
        Log.w(TAG, "Location providers disabled (GPS/Network)")
        throw IllegalStateException("Location disabled")
    }

    val fused = LocationServices.getFusedLocationProviderClient(ctx)
    val cts = CancellationTokenSource()
    val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

    // 1) Fresh fix
    @SuppressLint("MissingPermission")
    val fresh = runCatching {
        Log.d(TAG, "getCurrentLocation(priority=$priority)")
        fused.getCurrentLocation(priority, cts.token).await()
    }.onFailure { Log.w(TAG, "getCurrentLocation error", it) }
        .getOrNull()
    if (fresh != null) {
        Log.d(TAG, "Fresh fix: ${fresh.latitude}, ${fresh.longitude}")
        return Location(fresh.latitude, fresh.longitude)
    }

    // 2) Last known
    @SuppressLint("MissingPermission")
    val last = runCatching {
        Log.d(TAG, "lastLocation()")
        fused.lastLocation.await()
    }.onFailure { Log.w(TAG, "lastLocation error", it) }
        .getOrNull()
    if (last != null) {
        Log.d(TAG, "Last known: ${last.latitude}, ${last.longitude}")
        return Location(last.latitude, last.longitude)
    }

    // 3) Single update
    Log.d(TAG, "awaitSingleUpdate()")
    val single = runCatching { awaitSingleUpdate(fused, priority) }
        .onFailure { Log.w(TAG, "single update error", it) }
        .getOrNull()
    if (single != null) {
        Log.d(TAG, "Single update: ${single.latitude}, ${single.longitude}")
        return Location(single.latitude, single.longitude)
    }

    Log.e(TAG, "No location available after all strategies")
    error("No location available")
}

private fun isLocationEnabled(ctx: Context): Boolean {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@SuppressLint("MissingPermission")
private suspend fun awaitSingleUpdate(
    fused: FusedLocationProviderClient,
    priority: Int
): android.location.Location = suspendCancellableCoroutine { cont ->
    val req = LocationRequest.Builder(priority, /* interval */ 1000L)
        .setGranularity(
            if (priority == Priority.PRIORITY_HIGH_ACCURACY)
                Granularity.GRANULARITY_FINE else Granularity.GRANULARITY_COARSE
        )
        .setMaxUpdates(1)
        .setWaitForAccurateLocation(priority == Priority.PRIORITY_HIGH_ACCURACY)
        .build()

    val cb = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val l = result.lastLocation
            if (l != null && cont.isActive) cont.resume(l)
            fused.removeLocationUpdates(this)
        }
    }
    fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
    cont.invokeOnCancellation { fused.removeLocationUpdates(cb) }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
