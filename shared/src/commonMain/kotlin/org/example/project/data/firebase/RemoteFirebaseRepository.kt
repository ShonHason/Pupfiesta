// shared/src/commonMain/kotlin/org/example/project/data/firebase/RemoteFirebaseRepository.kt
package org.example.project.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.DogGarden
import org.example.project.domain.models.User
import org.example.project.domain.models.dogError
import org.example.project.platformLogger
import org.example.project.utils.extension.toDto
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
private const val COLL_USERS = "Users"
private const val COLL_DOGS = "Dogs"
private const val COLL_DOG_GARDENS = "DogGardens"

class RemoteFirebaseRepository : FirebaseRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val usersCol = db.collection(COLL_USERS)
    private val dogsCol = db.collection(COLL_DOGS)
    private val gardensCol = db.collection(COLL_DOG_GARDENS)

    private suspend fun currentUid(): String =
        auth.currentUser?.uid ?: throw Exception("Not signed in")

    // ──────────────────────────────
    // Auth / User Profile
    // ──────────────────────────────

    override suspend fun userLogin(email: String, password: String): Result<Unit, AuthError> {
        return try {
            auth.signInWithEmailAndPassword(email, password)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Login failed"))
        }
    }

    override suspend fun userRegistration(
        email: String,
        password: String,
        name: String,
        dogs: List<DogDto>
    ): Result<Unit, AuthError> {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, password)
            val uid = res.user?.uid ?: return Result.Failure(AuthError("Registration failed: missing uid"))
            // Store a basic user profile. If your UserDto has different fields, adjust accordingly.
            val userDto = UserDto(email = email, name = name, dogList = dogs)
            usersCol.document(uid).set(userDto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Registration failed"))
        }
    }

    override suspend fun logout(): Result<Unit, AuthError> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Logout failed"))
        }
    }

    override suspend fun updateUser(user: User): Result<Unit, AuthError> {
        return try {
            val uid = currentUid()
            // Map your domain User -> UserDto using your extension
            val dto = user.toDto()
            usersCol.document(uid).set(dto, merge = true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Update failed"))
        }
    }

    override suspend fun getUserProfile(): Result<UserDto, AuthError> {
        return try {
            val uid = currentUid()
            val snap = usersCol.document(uid).get()
            if (!snap.exists) {
                Result.Failure(AuthError("User not found"))
            } else {
                val user = snap.data<UserDto>()
                Result.Success(user)
            }
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "getUserProfile failed"))
        }
    }

    // ──────────────────────────────
    // Dogs (update Dogs collection + mirror in Users dogList)
    // ──────────────────────────────

// RemoteFirebaseRepository.kt  (inside addDogAndLinkToUser)

    override suspend fun addDogAndLinkToUser(dog: DogDto): Result<DogDto, dogError> = try {
        val uid = currentUid()

        // create dog in Dogs with generated id
        val draft = dog.copy(id = "", ownerId = uid)
        val ref = dogsCol.add(draft)
        val newId = ref.id
        val saved = draft.copy(id = newId)

        // mirror into user.dogList (REPLACE any legacy entry: same name + blank id)
        val userRef = usersCol.document(uid)
        val snap = userRef.get()
        val user = if (snap.exists) snap.data<UserDto>()
        else UserDto(email = auth.currentUser?.email ?: "", name = auth.currentUser?.displayName ?: "", dogList = emptyList())

        val newList = (user.dogList ?: emptyList())
            .filterNot {
                it.id == newId || (it.id.isNullOrBlank() && it.name.equals(draft.name, ignoreCase = true))
            } + saved

        userRef.set(user.copy(dogList = newList), merge = true)
        Result.Success(saved)
    } catch (e: Exception) {
        Result.Failure(dogError(e.message ?: "Add dog failed"))
    }

    override suspend fun updateDogAndUser(dog: DogDto): Result<Unit, dogError> {
        return try {
            val uid = currentUid()

            // Prefer dog.id, else resolve by (ownerId + name)
            val resolvedId: String = if (!dog.id.isNullOrBlank()) {
                dog.id!!
            } else {
                val q = dogsCol
                    .where { "ownerId" equalTo uid }
                    .where { "name" equalTo dog.name }
                    .get()
                q.documents.firstOrNull()?.id
                    ?: throw IllegalStateException("Dog id missing and no existing dog matched by name")
            }

            val withId = dog.copy(id = resolvedId, ownerId = uid)

            // Update Dogs/{id}
            dogsCol.document(resolvedId).set(withId, merge = true)

            // Mirror into Users/{uid}.dogList
            val userRef = usersCol.document(uid)
            val snap = userRef.get()
            if (snap.exists) {
                val user = snap.data<UserDto>()
                val src = user.dogList ?: emptyList()

                val updated =
                    if (src.any { it.id == resolvedId }) {
                        src.map { if (it.id == resolvedId) withId else it }
                    } else if (src.any { it.id.isNullOrBlank() && it.name.equals(dog.name, ignoreCase = true) }) {
                        // backfill legacy entry that had no id
                        src.map {
                            if (it.id.isNullOrBlank() && it.name.equals(dog.name, ignoreCase = true)) withId else it
                        }
                    } else {
                        src + withId
                    }

                userRef.set(user.copy(dogList = updated), merge = true)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(dogError(e.message ?: "Update failed"))
        }
    }


    override suspend fun deleteDogAndUser(dogId: String): Result<Unit, dogError> {
            platformLogger("FIREBASE", "Deleting dog $dogId")
            return try {
                val uid = currentUid()

                // Delete from Dogs
                dogsCol.document(dogId).delete()

                // Remove from user's dogList
            val userRef = usersCol.document(uid)
            val snap = userRef.get()
            if (snap.exists) {
                val user = snap.data<UserDto>()
                val updated = (user.dogList ?: emptyList()).filterNot { it.id == dogId }
                userRef.set(user.copy(dogList = updated), merge = true)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(dogError(e.message ?: "Delete failed"))
        }
    }

    // ──────────────────────────────
    // Dog gardens
    // ──────────────────────────────

    override suspend fun saveDogGardens(items: List<DogGarden>): Result<Unit, AuthError> {
        return try {
            for (g in items) {
                // If you want auto-id when id empty, do:
                if (g.id.isNotBlank()) {
                    gardensCol.document(g.id).set(g, merge = true)
                } else {
                    val ref = gardensCol.add(g)
                    gardensCol.document(ref.id).set(g.copy(id = ref.id), merge = true)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Save gardens failed"))
        }
    }

    override suspend fun getDogGardens(): Result<List<DogGarden>, AuthError> {
        return try {
            val snap = gardensCol.get()
            val list = snap.documents.mapNotNull { it.data<DogGarden>() }
            Result.Success(list)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Fetch gardens failed"))
        }
    }

    override suspend fun getDogGardensNear(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): Result<List<DogGarden>, AuthError> = try {
        val snap = gardensCol.get()
        val all = snap.documents.mapNotNull { it.data<DogGarden>() }

        // If your DogGarden has location { latitude, longitude }
        val nearby = all.filter { g ->
            val dist = distanceMeters(
                latitude,
                longitude,
                g.location.latitude,
                g.location.longitude
            )
            dist <= radiusMeters
        }

        Result.Success(nearby)
    } catch (e: Exception) {
        Result.Failure(AuthError(e.message ?: "Nearby fetch failed"))
    }

    // ──────────────────────────────
    // Helpers
    // ──────────────────────────────

    private fun toRad(d: Double) = d * PI / 180.0

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = toRad(lat2 - lat1)
        val dLon = toRad(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(toRad(lat1)) * cos(toRad(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6_371_000.0 * c
    }

}
