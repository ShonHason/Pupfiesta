import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.example.project.data.dogGardens.GardensRepository
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.local.Result
import org.example.project.domain.models.DogGarden
import org.example.project.features.BaseViewModel
import org.example.project.features.postAuth.PostAuthState
import org.example.project.features.registration.UserViewModel
import org.example.project.platformLogger
import org.example.project.utils.Location
import org.example.project.utils.getLocation

class PostAuthViewModel(
    private val gardensRepo: GardensRepository,
    private val firebaseRepo: FirebaseRepository,
    private val userViewModel: UserViewModel,
    private val defaultLanguage: String = "he"
) : BaseViewModel() {

    private val _state = MutableStateFlow<PostAuthState>(PostAuthState.Idle)
    val state: StateFlow<PostAuthState> = _state.asStateFlow()

    /**
     * Runs once after successful login/registration:
     * 1) Get location (or fallbackCenter)
     * 2) Search dog parks
     * 3) Save to Firestore
     * 4) Refresh user & open dog picker so the just-created dog appears
     */
    fun run(
        radiusMeters: Int = 11_000,
        locationTimeoutMs: Long = 6_000,
        language: String = defaultLanguage,
        fallbackCenter: Location? = null
    ) {
        scope.launch {
            try {
                _state.value = PostAuthState.Running("Getting location…")

                // Try fresh fix; if failed, use fallbackCenter if provided
                val fix = awaitLocationOrNull(locationTimeoutMs) ?: fallbackCenter
                if (fix == null) {
                    _state.value = PostAuthState.Error("Location unavailable")
                    return@launch
                }
                platformLogger("PUP", "PostAuthVM.run(): center=(${fix.latitude},${fix.longitude}) r=$radiusMeters")

                _state.value = PostAuthState.Running("Searching dog parks…")
                val parks: List<DogGarden> = runCatching {
                    gardensRepo.searchDogParks(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        radiusMeters = radiusMeters,
                        language = language
                    )
                }.getOrElse { t ->
                    _state.value = PostAuthState.Error("Search failed: ${t.message}")
                    return@launch
                }

                _state.value = PostAuthState.Running("Saving to database…")
                when (val res = firebaseRepo.upsertDogGardensCore(parks)) {
                    is Result.Failure -> {
                        _state.value = PostAuthState.Error(res.error?.message ?: "Save failed")
                        return@launch
                    }
                    is Result.Success -> {
                        // continue
                    }
                }

                // ✅ NEW: ensure the newly created dog is visible & selectable on Home
                _state.value = PostAuthState.Running("Refreshing your profile…")
                refreshUserAndOpenPickerIfPossible()

                _state.value = PostAuthState.Success(parks.size)
            } catch (t: Throwable) {
                _state.value = PostAuthState.Error(t.message ?: "Unexpected error")
            }
        }
    }

    fun reset() { _state.value = PostAuthState.Idle }

    private suspend fun awaitLocationOrNull(timeoutMs: Long): Location? =
        runCatching { withTimeoutOrNull(timeoutMs) { getLocation() } }.getOrNull()

    /**
     * Refresh current user and, if dogs exist, pre-select the first dog & open the picker.
     * Includes a short retry to handle eventual consistency right after registration.
     */
    private suspend fun refreshUserAndOpenPickerIfPossible() {

        val first = userViewModel.refreshUser()
        userViewModel.showDogPicker()  //

        fun selectFirst(dogs: List<org.example.project.data.remote.dto.DogDto>) {
            if (dogs.isNotEmpty()) {
                userViewModel.setSelectedDogIds(setOf(dogs.first().id))
            }
        }

        when (first) {
            is Result.Success -> selectFirst(first.data?.dogList.orEmpty())
            is Result.Failure -> platformLogger("PUP","PostAuthVM: refreshUser failed: ${first.error?.message}")
        }


        val delays = listOf(400L, 800L, 1500L)
        for (d in delays) {
            kotlinx.coroutines.delay(d)
            when (val r = userViewModel.refreshUser()) {
                is Result.Success -> {
                    val dogs = r.data?.dogList.orEmpty()
                    if (dogs.isNotEmpty()) {
                        selectFirst(dogs)
                        break
                    }
                }
                is Result.Failure -> {
                    platformLogger("PUP","PostAuthVM: retry refreshUser failed: ${r.error?.message}")
                }
            }
        }
    }
}