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

import kotlin.math.pow

import org.example.project.domain.models.Location as ModelLocation
import kotlinx.coroutines.Job
import org.example.project.data.remote.dto.DogDto

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
    private var searchJob: Job? = null
    private val nameCache = mutableMapOf<String, String>()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private var presenceJob: Job? = null
    private val _presentDogs = MutableStateFlow<List<DogDto>>(emptyList())
    val presentDogs: StateFlow<List<DogDto>> = _presentDogs.asStateFlow()

    private val _gardenPhotoUrl = MutableStateFlow<String?>(null)
    val gardenPhotoUrl: StateFlow<String?> = _gardenPhotoUrl.asStateFlow()


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

    fun startWatchingPresence(gardenId: String) {
        presenceJob?.cancel()
        presenceJob = scope.launch {
            firebaseRepo.listenGardenPresence(gardenId).collect { list ->
                _presentDogs.value = list
            }
        }
    }

    fun stopWatchingPresence() {
        presenceJob?.cancel()
        presenceJob = null
        _presentDogs.value = emptyList()
    }

    fun loadGardenPhoto(placeId: String, maxWidth: Int = 800) {
        scope.launch {
            _gardenPhotoUrl.value = runCatching {
                gardensRepo.getPlacePhotoUrl(placeId, maxWidth)
            }.getOrNull()
        }
    }

    suspend fun checkInDogs(gardenId: String, dogs: List<DogDto>) {
        firebaseRepo.checkInDogs(gardenId, dogs)
    }

    suspend fun checkOutDogs(gardenId: String, dogIds: List<String>) {
        firebaseRepo.checkOutDogs(gardenId, dogIds)
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
        // cancel previous to avoid races/flicker
        searchJob?.cancel()
        searchJob = scope.launch {
            val center = _searchCenter.value
            val radius = _radiusMeters.value
            try {
                val parks = gardensRepo.searchDogParks(
                    latitude = center.latitude,
                    longitude = center.longitude,
                    radiusMeters = radius,
                    language = defaultLanguage
                )

                // strict geo filter
                val filteredByDistance = parks.filter {
                    distanceMeters(center, it.location) <= radius
                }

                // stabilize name (avoid toggling back to "Dog Park")
                val stabilized = filteredByDistance.map { p ->
                    val current = p.name
                    val cached = nameCache[p.id]

                    val finalName =
                        when {
                            !current.isNullOrBlank() && !current.equals("Dog Park", ignoreCase = true) -> current
                            cached != null -> cached
                            else -> current ?: "Dog Park"
                        }

                    nameCache[p.id] = finalName
                    p.copy(name = finalName)
                }

                _gardens.value = stabilized
            } catch (_: Throwable) {
                _gardens.value = emptyList()
            }
        }
    }

    // ---------- distance helpers ----------
    // Overload to bridge utils.Location (center) and domain.models.Location (park)
    private fun distanceMeters(a: Location, b: ModelLocation): Int =
        distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)

    private fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Int {
        val R = 6_371_000.0
        fun Double.deg2rad() = this * kotlin.math.PI / 180.0
        val dLat = (lat2 - lat1).deg2rad()
        val dLon = (lon2 - lon1).deg2rad()
        val a = kotlin.math.sin(dLat / 2).pow(2.0) +
                kotlin.math.cos(lat1.deg2rad()) * kotlin.math.cos(lat2.deg2rad()) *
                kotlin.math.sin(dLon / 2).pow(2.0)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (R * c).toInt()
    }

    fun getGardens() {
        val pivot = _userLocation.value ?: _searchCenter.value
        val radius = _radiusMeters.value

        scope.launch {
            when (val result = firebaseRepo.getDogGardensNear(pivot.latitude, pivot.longitude, radius)) {
                is Result.Success -> _gardens.value = result.data ?: emptyList()
                is Result.Failure -> _gardens.value = emptyList()
            }
        }
    }

    private suspend fun getLocationOrNull(timeoutMs: Long): Location? =
        runCatching { withTimeoutOrNull(timeoutMs) { getLocation() } }.getOrNull()
}
