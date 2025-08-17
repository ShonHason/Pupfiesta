// shared/src/commonMain/kotlin/org/example/project/features/dogGardens/DogGardensViewModel.kt
package org.example.project.data.dogGardens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.local.Result
import org.example.project.domain.models.DogGarden
import org.example.project.features.BaseViewModel
import org.example.project.platformLogger
import org.example.project.utils.Location
import org.example.project.utils.getLocation

/**
 * Prefer REAL device location. If unavailable/denied/slow, fall back to TEL_AVIV,
 * but keep auto-centering to REAL location once it becomes available.
 */
class DogGardensViewModel(
    private val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository(),
    private val gardensRepo: GardensRepository,
    private val defaultLanguage: String = "he"
) : BaseViewModel() {

    private val TEL_AVIV = Location(32.0853, 34.7818)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Start with TA so UI isn't empty; update to real GPS as soon as we get a fix
    private val _searchCenter = MutableStateFlow(TEL_AVIV)
    val searchCenter: StateFlow<Location> = _searchCenter.asStateFlow()

    private val _radiusMeters = MutableStateFlow(1_000)
    val radiusMeters: StateFlow<Int> = _radiusMeters.asStateFlow()

    private val _gardens = MutableStateFlow<List<DogGarden>>(emptyList())
    val gardens: StateFlow<List<DogGarden>> = _gardens.asStateFlow()

    private var locating = false
    private var didCenterOnRealOnce = false
    private var didFallbackTelAvivOnce = false

    // ---- replace your loadLocation() with this version ----
    fun loadLocation() {
        platformLogger("PUP","loadLocation() START  userLocation=${_userLocation.value}  searchCenter=${_searchCenter.value}")

        if (locating) return
        locating = true
        scope.launch {
            try {
                platformLogger("PUP","loadLocation() START  userLocation=${_userLocation.value}  searchCenter=${_searchCenter.value}")

                // allow a bit longer for the very first GPS fix
                val loc = getLocationOrNull(timeoutMs = 6000)

                if (loc != null) {
                    _userLocation.value = loc
                    _searchCenter.value = loc
                    platformLogger("PUP","loadLocation() GOT FIX   lat=${loc.latitude}, lon=${loc.longitude} | center=${_searchCenter.value}")

                    if (!didCenterOnRealOnce) {
                        didCenterOnRealOnce = true
                        getGoogleGardens()
                    }
                } else {
                    platformLogger("PUP","loadLocation() NO FIX (timeout/denied). center stays=${_searchCenter.value}")
                    // show Tel-Aviv once so UI isn't empty; do NOT block later real fix
                    if (!didFallbackTelAvivOnce) {
                        _searchCenter.value = TEL_AVIV
                        didFallbackTelAvivOnce = true
                        platformLogger("PUP","loadLocation() FALLBACK -> TEL_AVIV | center=${_searchCenter.value}")
                        getGoogleGardens()
                    }
                }
            } catch (t: Throwable) {
                platformLogger("PUP","loadLocation() ERROR: ${t.message}")
            } finally {
                locating = false
            }
        }
    }


    fun onScanClick() {
        scope.launch {
            val fresh = getLocationOrNull(timeoutMs = 3000)
            if (fresh != null) {
                platformLogger("PUP", "onScanClick() - got fresh user location: lat=${fresh.latitude}, lon=${fresh.longitude}")
                _userLocation.value = fresh
                _searchCenter.value = fresh
                if (!didCenterOnRealOnce) didCenterOnRealOnce = true
            } else {
                _searchCenter.value = _userLocation.value ?: TEL_AVIV
            }
            getGoogleGardens()
        }
    }

    fun setSearchCenter(location: Location) { _searchCenter.value = location }
    fun useTelAvivAsCenter() { _searchCenter.value = TEL_AVIV }
    fun useMyLocationAsCenter() { _userLocation.value?.let { _searchCenter.value = it } }
    fun setRadius(meters: Int) { _radiusMeters.value = meters.coerceIn(100, 50_000) }

    fun loadGardensAround(latitude: Double, longitude: Double, radius: Int) {
        _searchCenter.value = Location(latitude, longitude)
        setRadius(radius)
        getGoogleGardens()
    }

    // In DogGardensViewModel
    fun refreshAndSave() {
        scope.launch {
            val center = _searchCenter.value
            val radius = _radiusMeters.value

            val parks = try {
                gardensRepo.searchDogParks(
                    latitude = center.latitude,
                    longitude = center.longitude,
                    radiusMeters = radius,
                    language = defaultLanguage
                )
            } catch (t: Throwable) {
                platformLogger("PUP","fetch parks failed: ${t.message}")
                emptyList()
            }

            _gardens.value = parks

            val res = firebaseRepo.saveDogGardens(parks)
            when (res) {
                is Result.Success -> platformLogger("PUP","Saved ${parks.size} gardens to Firestore")
                is Result.Failure -> platformLogger("PUP","Save failed: ${res.error?.message}")
            }
        }
    }

    fun getGoogleGardens() {
        scope.launch {
            val center = _searchCenter.value
            val radius = _radiusMeters.value
            try {
                val parks = gardensRepo.searchDogParks(
                    latitude = center.latitude,
                    longitude = center.longitude,
                    radiusMeters = radius,
                    language = defaultLanguage
                )
                _gardens.value = parks
            } catch (_: Throwable) {
                _gardens.value = emptyList()
            }
        }
    }

    fun getGardens() {
        val pivot = _userLocation.value ?: _searchCenter.value
        val radius = _radiusMeters.value


        scope.launch {
            when (val result = firebaseRepo.getDogGardensNear(pivot.latitude,pivot.longitude, radius)) {
                is Result.Success -> _gardens.value = result.data ?: emptyList()
                is Result.Failure -> _gardens.value = emptyList()
            }
        }
    }

    private suspend fun getLocationOrNull(timeoutMs: Long): Location? =
        runCatching { withTimeoutOrNull(timeoutMs) { getLocation() } }.getOrNull()
}
