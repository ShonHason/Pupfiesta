package org.example.project.data.dogs

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.dogError
import org.example.project.features.BaseViewModel
import org.example.project.platformLogger

/**
 * Dogs ViewModel – mirrors your UserViewModel.
 * Exposes Result-returning API + OrThrow variants for Swift.
 */
class DogsViewModel(
    private val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository()
) : BaseViewModel() {

    private val _dogsState = MutableStateFlow<DogsState>(DogsState.Initial(DogFormData()))
    val dogsState: StateFlow<DogsState> = _dogsState

    // ─────────────── Setters ───────────────

    fun setId(v: String)               = onEvent(DogsEvent.IdChanged(v))
    fun setName(v: String)             = onEvent(DogsEvent.NameChanged(v))
    fun setBreed(v: org.example.project.enum.Breed?) =
        onEvent(DogsEvent.BreedChanged(v))
    fun setWeight(kg: Int)             = onEvent(DogsEvent.WeightChanged(kg))
    fun setImageUrl(url: String?)      = onEvent(DogsEvent.ImageUrlChanged(url))
    fun setIsFriendly(v: Boolean)      = onEvent(DogsEvent.IsFriendlyChanged(v))
    fun setIsMale(v: Boolean)          = onEvent(DogsEvent.IsMaleChanged(v))
    fun setIsNeutered(v: Boolean)      = onEvent(DogsEvent.IsNeuteredChanged(v))

    fun reset()                        = onEvent(DogsEvent.ResetState)
    fun loadFromDto(d: DogDto)         = onEvent(DogsEvent.LoadFromDto(d))
    fun save()                         = onEvent(DogsEvent.OnSave)

    // ─────────────── Event reducer ───────────────

    fun onEvent(event: DogsEvent) {
        when (event) {
            is DogsEvent.IdChanged           -> updateState { copy(id = event.id) }
            is DogsEvent.NameChanged         -> updateState { copy(name = event.name) }
            is DogsEvent.BreedChanged        -> updateState { copy(breed = event.breed) }
            is DogsEvent.WeightChanged       -> updateState { copy(weight = event.kg) }
            is DogsEvent.ImageUrlChanged     -> updateState { copy(imgUrl = event.url) }
            is DogsEvent.IsFriendlyChanged   -> updateState { copy(isFriendly = event.isFriendly) }
            is DogsEvent.IsMaleChanged       -> updateState { copy(isMale = event.isMale) }
            is DogsEvent.IsNeuteredChanged   -> updateState { copy(isNeutered = event.isNeutered) }
            DogsEvent.ResetState             -> resetState()
            is DogsEvent.LoadFromDto         -> loadFormFromDto(event.dog)
            DogsEvent.OnSave                 -> validateAndSave()
        }
    }

    private fun updateState(transform: DogFormData.() -> DogFormData) {
        val current = (_dogsState.value as? DogsState.Initial)?.data ?: DogFormData()
        _dogsState.value = DogsState.Initial(current.transform())
    }

    private fun resetState() {
        _dogsState.value = DogsState.Initial(DogFormData())
    }

    private fun loadFormFromDto(d: DogDto) {
        platformLogger("PUP", "Loading dog form from DTO: $d")
        updateState {
            copy(
                id = d.id.orEmpty(),
                name = d.name,
                breed = d.breed,
                weight = d.weight,
                imgUrl = d.photoUrl.takeIf { it.isNotBlank() },
                isFriendly = d.isFriendly,
                isMale = d.isMale,
                isNeutered = d.isNeutered,
                ownerId = d.ownerId
            )
        }
    }

    // ─────────────── Validation + Save (Add/Update) ───────────────

    private fun validateAndSave() {
        val init = _dogsState.value as? DogsState.Initial ?: return
        val data = init.data

        val safeBreed = data.breed ?: org.example.project.enum.Breed.MIXED

        val errors = buildList {
            if (data.name.isBlank()) add("Dog name cannot be empty")
            if (data.weight !in 1..200) add("Weight must be between 1 and 200")
            // no error for null breed — we defaulted to MIXED
        }

        // reflect the default in UI
        updateState { copy(breed = safeBreed, errors = errors) }
        if (errors.isNotEmpty()) return

        val dto = DogDto(
            id            = data.id,
            name          = data.name,
            breed         = safeBreed,                       // <- no !!
            weight        = data.weight,
            dogPictureUrl = data.imgUrl.orEmpty(),
            isFriendly    = data.isFriendly,
            isMale        = data.isMale,
            isNeutered    = data.isNeutered,
            ownerId       = data.ownerId
        )

        scope.launch {
            _dogsState.value = DogsState.Loading
            val result: Result<*, *> =
                if (data.id.isBlank()) firebaseRepo.addDogAndLinkToUser(dto)
                else                    firebaseRepo.updateDogAndUser(dto)

            _dogsState.value = when (result) {
                is Result.Success -> {
                    delay(200)
                    DogsState.Loaded
                }
                is Result.Failure -> DogsState.Error(result.error?.toString() ?: "Save failed")
            }
        }
    }

    // ─────────────── Public API (Result style) ───────────────

    suspend fun getUser(): Result<UserDto, AuthError> {
        val r = firebaseRepo.getUserProfile()
        when (r) {
            is Result.Success -> _dogsState.value = DogsState.Loaded
            is Result.Failure -> _dogsState.value = DogsState.Error(r.error?.message ?: "Load failed")
        }
        return r
    }

    // New names matching your repository
    suspend fun addDogAndLinkToUser(dog: DogDto): Result<DogDto, dogError> =
        firebaseRepo.addDogAndLinkToUser(dog)

    suspend fun updateDogAndUser(dog: DogDto): Result<Unit, dogError> =
        firebaseRepo.updateDogAndUser(dog)

    suspend fun deleteDogAndUser(dogId: String): Result<Unit, dogError> =
        firebaseRepo.deleteDogAndUser(dogId)

    // ─────────────── Swift-friendly OrThrow API ───────────────

    @Throws(Exception::class)
    suspend fun getUserOrThrow(): UserDto {
        return when (val r = firebaseRepo.getUserProfile()) {

            is Result.Success -> r.data ?: throw Exception("Empty user payload")
            is Result.Failure -> throw Exception(r.error?.message ?: "Failed to get current user")
        }
    }

    @Throws(Exception::class)
    suspend fun addDogAndLinkToUserOrThrow(dog: DogDto): DogDto {
        return when (val r = firebaseRepo.addDogAndLinkToUser(dog)) {
            is Result.Success -> r.data ?: throw Exception("Empty dog payload")
            is Result.Failure -> throw Exception(r.error?.message ?: "Failed to add dog")
        }
    }

    @Throws(Exception::class)
    suspend fun updateDogAndUserOrThrow(dog: DogDto) {
        when (val r = firebaseRepo.updateDogAndUser(dog)) {
            is Result.Success -> Unit
            is Result.Failure -> throw Exception(r.error?.message ?: "Failed to update dog")
        }
    }

    @Throws(Exception::class)
    suspend fun deleteDogAndUserOrThrow(dogId: String) {
        when (val r = firebaseRepo.deleteDogAndUser(dogId)) {
            is Result.Success -> Unit
            is Result.Failure -> throw Exception(r.error?.message ?: "Failed to delete dog")
        }
    }
}
