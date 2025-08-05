package org.example.project.features.registration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.user.UserEvent
import org.example.project.data.user.UserFormData
import org.example.project.data.user.UserState
import org.example.project.enum.Gender
import org.example.project.features.BaseViewModel

class UserViewModel(
    private val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository()
) : BaseViewModel() {

    private val _userState =
        MutableStateFlow<UserState>(UserState.Initial(UserFormData()))
    val userState: StateFlow<UserState> = _userState

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
            is UserEvent.DogPictureUrlChanged -> updateState { copy(dogPictureUrl = event.dogPictureUrl) }
            UserEvent.ResetState           -> resetState()
            UserEvent.OnSignUp             -> validateAndSubmitForm()
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
            imgUrl     = data.dogPictureUrl.orEmpty(),
            isFriendly = data.isFriendly,
            isMale     = (data.dogGender == Gender.MALE),
            isNeutered = data.isNeutered,
            ownerId    = ""
        )

        // 3️⃣ Launch registration
        scope.launch {
            _userState.value = UserState.Loading

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

    private fun validateEmail(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email.trim())
    }

    private fun validatePassword(password: String): Boolean =
        password.length >= 6
}
