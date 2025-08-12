// shared/src/commonMain/kotlin/org/example/project/features/dogGardens/DogGardensViewModel.kt
package org.example.project.data.dogGardens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.local.Result
import org.example.project.domain.models.DogGarden
import org.example.project.features.BaseViewModel
import org.example.project.utils.Location
import org.example.project.utils.getLocation

class DogGardensViewModel(
    private val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository(),
    private val gardensRepo: GardensRepository,
    private val defaultLanguage: String = "he"
) : BaseViewModel() {

    private val TEL_AVIV = Location(32.0853, 34.7818)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _searchCenter = MutableStateFlow<Location>(TEL_AVIV)
    val searchCenter: StateFlow<Location> = _searchCenter.asStateFlow()

    private val _radiusMeters = MutableStateFlow(10_000)
    val radiusMeters: StateFlow<Int> = _radiusMeters.asStateFlow()

    private val _gardens = MutableStateFlow<List<DogGarden>>(emptyList())
    val gardens: StateFlow<List<DogGarden>> = _gardens.asStateFlow()

        fun loadLocation() {
            scope.launch {
                try { _userLocation.value = getLocation() } catch (_: Throwable) { _userLocation.value = null }
            }
        }

    fun setSearchCenter(location: Location) { _searchCenter.value = location }
    fun useTelAvivAsCenter() { _searchCenter.value = TEL_AVIV }
    fun useMyLocationAsCenter() { _userLocation.value?.let { _searchCenter.value = it } }

    fun setRadius(meters: Int) { _radiusMeters.value = meters.coerceIn(100, 50_000) }

    /** Load dog gardens around arbitrary center & radius (e.g., Tel Aviv + X meters) */
    fun loadGardensAround(latitude: Double, longitude: Double, radius: Int) {
        _searchCenter.value = Location(latitude, longitude)
        setRadius(radius)
        refreshGardensFromGoogle()
    }

    /** Fetch from Google using current center & radius */
    fun refreshGardensFromGoogle() {
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

    /** Optional: persist current list to Firebase */
    fun saveGardens() {
        scope.launch {
            when (firebaseRepo.saveDogGardens(_gardens.value)) {
                is Result.Success -> Unit
                is Result.Failure -> Unit
            }
        }
    }
    fun getGardens() {
        scope.launch {
            when (val result = firebaseRepo.getDogGardens()) {
                is Result.Success -> _gardens.value = result.data?: emptyList()
                is Result.Failure -> _gardens.value = emptyList()
            }
        }
    }
}
