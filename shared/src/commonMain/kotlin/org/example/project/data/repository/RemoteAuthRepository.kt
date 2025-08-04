package org.example.project.data.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.repository.AuthRepository

import org.example.project.domain.models.User
import org.example.project.utils.extension.toDto

class RemoteAuthRepository : AuthRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    override suspend fun userLogin(
        email: String,
        password: String
    ): Result<Unit, AuthError> {
        return try {
            auth.signInWithEmailAndPassword(email, password)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError("Login failed: ${e.message}"))
        }
    }
    override suspend fun userRegistration(
        email: String,
        password: String,
        name: String,
        dogs: List<DogDto>
    ): Result<Unit, AuthError> {
        return try {
            // 1️⃣ Create the Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password)
            val fbUser = authResult.user
                ?: return Result.Failure(AuthError("No UID returned"))
            fbUser.updateProfile(displayName = name)
            val uid = fbUser.uid

            // 2️⃣ Sequentially write each dog into /Dogs
            val createdDogs = mutableListOf<DogDto>()
            for (dog in dogs) {
                val dogWithOwner = dog.copy(ownerId = uid)
                // add() returns DocumentReference
                val ref = firestore.collection("Dogs").add(dogWithOwner)
                val generatedId = ref.id
                // set the id field on that doc
                firestore.collection("Dogs")
                    .document(generatedId)
                    .set(dogWithOwner.copy(id = generatedId), merge = true)
                createdDogs += dogWithOwner.copy(id = generatedId)
            }

            // 3️⃣ Write the Users/{uid} doc with embedded dogList
            val userDto = UserDto(
                email   = email,
                name    = name,
                dogList = createdDogs
            )
            firestore
                .collection("Users")
                .document(uid)
                .set(userDto)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError("Registration failed: ${e.message}"))
        }
    }


    override suspend fun getCurrentUser(): Result<String, AuthError> {
        val user = auth.currentUser
        return if (user != null) {
            Result.Success(user.uid)
        } else {
            Result.Failure(AuthError("No signed-in user"))
        }
    }


    override suspend fun updateUser(
        user: User
    ): Result<Unit, AuthError> {
        // 1️⃣ Ensure a user is signed in
        val fbUser = Firebase.auth.currentUser
            ?: return Result.Failure(AuthError("No user signed in"))

        return try {
            // 2️⃣ Sync Auth profile if it changed
            if (fbUser.displayName != user.ownerName) {
                fbUser.updateProfile(displayName = user.ownerName)
            }
            if (fbUser.email != user.email) {
                fbUser.updateEmail(user.email)
            }

            val uid = fbUser.uid
            val userRef = Firebase.firestore
                .collection("Users")
                .document(uid)

            // 3️⃣ Fetch the existing UserDto from Firestore
            //    (uses the reified data<T>() extension)
            val existingDto: UserDto = userRef.get().data()

            // 4️⃣ Compare its dogList with the new one
            val oldDogs: List<DogDto> = existingDto.dogList
            val newDto: UserDto = user.toDto()
            val newDogs: List<DogDto> = newDto.dogList

            val needsDogUpdate = oldDogs.size != newDogs.size ||
                    oldDogs.zip(newDogs).any { (oldDog, newDog) ->
                        oldDog.name != newDog.name ||
                                oldDog.breed != newDog.breed ||
                                oldDog.weight != newDog.weight ||
                                oldDog.imgUrl != newDog.imgUrl ||
                                oldDog.isFriendly != newDog.isFriendly ||
                                oldDog.isMale != newDog.isMale ||
                                oldDog.isNeutered != newDog.isNeutered
                    }

            // 5️⃣ Build the merge‐payload
            val payload = mutableMapOf<String, Any>(
                "email" to user.email,
                "name" to user.ownerName
            )
            if (needsDogUpdate) {
                payload["dogList"] = newDogs
            }

            // 6️⃣ Write it back
            userRef.set(payload, merge = true)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError("Update failed: ${e.message}"))
        }
    }

    override suspend fun logout(): Result<Unit, AuthError> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError("Logout failed: ${e.message}"))
        }
    }
}
