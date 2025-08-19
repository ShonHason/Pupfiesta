package org.example.project.features.registration

import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.data.user.UserEvent
import org.example.project.data.user.UserFormData
import org.example.project.data.user.UserState
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.User
import org.example.project.domain.models.dogError
import org.example.project.enum.Gender
import org.example.project.features.BaseViewModel
import org.example.project.platformLogger

class UserViewModel(
    private val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository(),



) : BaseViewModel() {

    private val _userState =
        MutableStateFlow<UserState>(UserState.Initial(UserFormData()))
    val userState: StateFlow<UserState> = _userState

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _selectedDogIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDogIds: StateFlow<Set<String>> = _selectedDogIds.asStateFlow()

    private val _shouldShowDogPicker = MutableStateFlow(false)
    val shouldShowDogPicker: StateFlow<Boolean> = _shouldShowDogPicker.asStateFlow()

    fun setEmail(v: String) =
        onEvent(UserEvent.EmailChanged(v))

    fun setPassword(v: String) =
        onEvent(UserEvent.PasswordChanged(v))

    fun setOwnerName(v: String) =
        onEvent(UserEvent.OwnerNameChanged(v))

    fun setDogName(v: String) =
        onEvent(UserEvent.DogNameChanged(v))

    fun setDogBreed(breed: org.example.project.enum.Breed) =
        onEvent(UserEvent.DogBreedChanged(breed))

    fun setDogGender(gender: org.example.project.enum.Gender) =
        onEvent(UserEvent.DogGenderChanged(gender))

    fun setIsFriendly(v: Boolean) =
        onEvent(UserEvent.IsFriendlyChanged(v))

    fun setIsNeutered(v: Boolean) =
        onEvent(UserEvent.IsNeuteredChanged(v))

    fun setDogWeight(kg: Int) =
        onEvent(UserEvent.DogWeightChanged(kg))

    fun setDogPictureUrl(url: String?) =
        onEvent(UserEvent.DogPictureUrlChanged(url))

    // --- Actions ---
    fun reset() =
        onEvent(UserEvent.ResetState)

    fun signUp() =
        onEvent(UserEvent.OnSignUp)

    fun signIn() =
        onEvent(UserEvent.OnSignIn)

    fun onEvent(event: UserEvent) {
        when (event) {
            is UserEvent.EmailChanged      -> updateState { copy(email = event.email) }
            is UserEvent.PasswordChanged   -> updateState { copy(password = event.password) }
            is UserEvent.OwnerNameChanged  -> updateState { copy(ownerName = event.ownerName) }
            is UserEvent.DogNameChanged    -> updateState { copy(dogName = event.dogName) }
            is UserEvent.DogBreedChanged   -> updateState { copy(dogBreed = event.dogBreed) }
            is UserEvent.DogGenderChanged  -> updateState { copy(dogGender = event.dogGender) }
            is UserEvent.IsFriendlyChanged -> updateState { copy(isFriendly = event.isFriendly) }
            is UserEvent.IsNeuteredChanged -> updateState { copy(isNeutered = event.isNeutered) }
            is UserEvent.DogWeightChanged  -> updateState { copy(dogWeight = event.dogWeight) }
            UserEvent.ResetState           -> resetState()
            UserEvent.OnSignUp             -> validateAndSubmitForm()
            is UserEvent.OnSignIn -> userSignIn()
            is UserEvent.DogPictureUrlChanged -> updateState { copy(dogPictureUrl = event.url) }
        }
    }

    suspend fun loadUserIfNeeded(): Result<UserDto, AuthError> {
        _currentUser.value?.let { return Result.Success(it) }
        val r = firebaseRepo.getUserProfile()
        when (r) {
            is Result.Success -> {
                _currentUser.value = r.data
                _userState.value = UserState.Loaded
            }
            is Result.Failure -> {
                _userState.value = UserState.Error(r.error?.message ?: "Login failed")
            }
        }
        return r
    }
    suspend fun refreshUser(): Result<UserDto, AuthError> {
        val r = firebaseRepo.getUserProfile()
        when (r) {
            is Result.Success -> {
                _currentUser.value = r.data
                _userState.value = UserState.Loaded
            }
            is Result.Failure -> {
                _userState.value = UserState.Error(r.error?.message ?: "Load failed")
            }
        }
        return r
    }
    fun invalidateUserCache() { _currentUser.value = null }


    @Throws(Exception::class)
    suspend fun getUserOrThrow(): UserDto {
        return when (val r = firebaseRepo.getUserProfile()) {
            is Result.Success -> r.data ?: throw Exception("Empty user payload")
            is Result.Failure -> throw Exception(r.error?.message ?: "Failed to get current user")
        }
    }

    suspend fun getUser(): Result<UserDto, AuthError> {
        val r = firebaseRepo.getUserProfile()
        when (r) {
            is Result.Success -> {
                _userState.value = UserState.Loaded
            }
            is Result.Failure -> {
                _userState.value = UserState.Error(r.error?.message ?: "Login failed")
            }
        }
        return r
    }




    private fun userSignIn() {
        val initState = _userState.value as? UserState.Initial ?: return
        val data = initState.data

        scope.launch {
            // show the loader immediately
            _userState.value = UserState.Loading

            // perform the login call
            val result = firebaseRepo.userLogin(
                email = data.email,
                password = data.password
            )

            when (result) {
                is Result.Success -> {
                    // keep the loader on-screen for at least 2 seconds
                    delay(3000)
                    _userState.value = UserState.Loaded
                    showDogPicker()
                    scope.launch { loadUserIfNeeded() }

                }
                is Result.Failure -> {
                    // no delay on failure, show error right away
                    _userState.value = UserState.Error(
                        result.error?.message ?: "Login failed"
                    )
                }
            }
        }
    }

    private fun updateState(transform: UserFormData.() -> UserFormData) {
        val current = (_userState.value as? UserState.Initial)?.data
            ?: UserFormData()
        _userState.value = UserState.Initial(current.transform())
    }



    private fun resetState() {
        _userState.value = UserState.Initial(UserFormData())
    }

    private fun validateAndSubmitForm() {
        val initState = _userState.value as? UserState.Initial ?: return
        val data = initState.data

        // 1️⃣ Validate inputs
        val errors = mutableListOf<String>().apply {
            if (data.email.isBlank())             add("Email cannot be empty")
            if (!validateEmail(data.email))       add("Invalid email format")
            if (data.password.isBlank())          add("Password cannot be empty")
            if (!validatePassword(data.password)) add("Password must be at least 6 characters")
            if (data.ownerName.isBlank())         add("Owner name cannot be empty")
            if (data.dogName.isBlank())           add("Dog name cannot be empty")
        }
        updateState { copy(errors = errors) }
        if (errors.isNotEmpty()) return

        // 2️⃣ Build DogDto
        val dogDto = DogDto(
            id         = "",
            name       = data.dogName,
            breed      = data.dogBreed,
            weight     = data.dogWeight,
            dogPictureUrl     = data.dogPictureUrl.orEmpty(),
            isFriendly = data.isFriendly,
            isMale     = (data.dogGender == Gender.MALE),
            isNeutered = data.isNeutered,
            ownerId    = ""
        )

        // 3️⃣ Launch registration
        scope.launch {
            _userState.value = UserState.Loading

            platformLogger("PUP" , "Launching user registration with data: $data")
            when (val result = firebaseRepo.userRegistration(
                email    = data.email,
                password = data.password,
                name     = data.ownerName,
                dogs     = listOf(dogDto)


            )) {
                is Result.Success ->
                    _userState.value = UserState.Loaded
                is Result.Failure ->
                    _userState.value = UserState.Error(
                        result.error?.message ?: "Registration failed"
                    )
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        scope.launch {
            _userState.value = UserState.Loading

            when (val result = firebaseRepo.userLogin(email, password)) {
                is Result.Success ->
                    _userState.value = UserState.Loaded
                is Result.Failure ->
                    _userState.value = UserState.Error(
                        result.error?.message ?: "Login failed"
                    )
            }
        }
    }

    suspend fun addDog(
        name: String,
        breed: org.example.project.enum.Breed,
        weight: Int,
        isFriendly: Boolean,
        isMale: Boolean,
        isNeutered: Boolean,
        pictureUrl: String
    ): Result<DogDto, dogError> {
        val dto = DogDto(
            id = "",
            name = name.trim(),
            breed = breed,
            weight = weight,
            dogPictureUrl = pictureUrl.trim(),
            isFriendly = isFriendly,
            isMale = isMale,
            isNeutered = isNeutered,
            ownerId = "" // repo fills uid
        )
        return firebaseRepo.addDogAndLinkToUser(dto)
    }

    private fun validateEmail(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email.trim())
    }
    fun showDogPicker() {
        _shouldShowDogPicker.value = true
    }
    fun dismissDogPicker() {
        _shouldShowDogPicker.value = false
    }
    fun setSelectedDogIds(ids: Set<String>) {
        _selectedDogIds.value = ids
    }
    fun addSelectedDog(id: String) {
        _selectedDogIds.value = _selectedDogIds.value + id
    }

    fun removeSelectedDog(id: String) {
        _selectedDogIds.value = _selectedDogIds.value - id
    }

    fun toggleSelectedDog(id: String) {
        _selectedDogIds.value = _selectedDogIds.value.let { sel ->
            if (id in sel) sel - id else sel + id
        }
    }

    fun clearSelectedDogs() {
        _selectedDogIds.value = emptySet()
    }

    private fun validatePassword(password: String): Boolean =
        password.length >= 6
}
