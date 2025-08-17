// shared/src/commonMain/kotlin/org/example/project/data/firebase/RemoteFirebaseRepository.kt
package org.example.project.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.DogGarden
import org.example.project.domain.models.User
import org.example.project.domain.models.dogError
import org.example.project.platformLogger
import org.example.project.utils.extension.toDto
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLL_USERS = "Users"
private const val COLL_DOGS = "Dogs"
private const val COLL_DOG_GARDENS = "DogGardens"
private const val SUBCOLL_PRESENCE = "Presence"

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

    // Realtime presence fallback: polling Flow (works in KMP commonMain)
    override fun listenGardenPresence(gardenId: String): Flow<List<DogDto>> = flow {
        val presenceCol = gardensCol.document(gardenId).collection(SUBCOLL_PRESENCE)
        while (currentCoroutineContext().isActive) {
            val qs = presenceCol.get()
            val list = qs.documents.mapNotNull { doc -> doc.data<DogDto>() }
            emit(list)
            delay(2500) // tweak polling interval if you like
        }
    }

    override suspend fun checkInDogs(gardenId: String, dogs: List<DogDto>): Result<Unit, AuthError> =
        try {
            val uid = currentUid()
            val presenceCol = gardensCol.document(gardenId).collection(SUBCOLL_PRESENCE)
            dogs.forEach { d ->
                val docId = d.id.ifBlank { return@forEach }   // need dogId
                val payload = d.copy(ownerId = uid)            // lightweight doc for UI
                presenceCol.document(docId).set(payload, merge = true)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Check-in failed"))
        }

    override suspend fun checkOutDogs(gardenId: String, dogIds: List<String>): Result<Unit, AuthError> =
        try {
            val presenceCol = gardensCol.document(gardenId).collection(SUBCOLL_PRESENCE)
            dogIds.forEach { id -> presenceCol.document(id).delete() }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "Check-out failed"))
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

            val payload = mapOf(
                "id" to uid,
                "email" to email,
                "ownerName" to name,
                // Only store dog ids in Users/{uid}.dogList
                "dogList" to dogs.map { mapOf("id" to it.id) }
            )
            usersCol.document(uid).set(payload, merge = true)
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
            if (!snap.exists) return Result.Failure(AuthError("User not found"))

            var user = snap.data<UserDto>()

            // Ensure user.id is set (prefer field from doc, else fallback to uid)
            val storedId = runCatching { snap.get("id") as? String }.getOrNull()
            user = user.copy(id = if (user.id.isNotBlank()) user.id else (storedId ?: uid))

            // Enrich each dog from Dogs/{id}
            val enriched = user.dogList.map { d ->
                if (d.id.isBlank()) d
                else {
                    val dogSnap = dogsCol.document(d.id).get()
                    if (!dogSnap.exists) d
                    else {
                        val pic      = runCatching { dogSnap.get("dogPictureUrl") as? String }.getOrNull() ?: d.dogPictureUrl
                        val name     = runCatching { dogSnap.get("name") as? String }.getOrNull() ?: d.name
                        val breedStr = runCatching { dogSnap.get("breed") as? String }.getOrNull()
                        val breed    = breedStr?.let { runCatching { org.example.project.enum.Breed.valueOf(it) }.getOrNull() } ?: d.breed
                        val weight   = runCatching { dogSnap.get("weight") as? Long }.getOrNull()?.toInt() ?: d.weight
                        val owner    = runCatching { dogSnap.get("ownerId") as? String }.getOrNull() ?: d.ownerId

                        d.copy(
                            dogPictureUrl = pic,
                            name = name,
                            breed = breed,
                            weight = weight,
                            ownerId = owner
                        )
                    }
                }
            }

            Result.Success(user.copy(dogList = enriched))
        } catch (e: Exception) {
            Result.Failure(AuthError(e.message ?: "getUserProfile failed"))
        }
    }

    // ──────────────────────────────
    // Dogs (update Dogs collection + mirror in Users dogList)
    // ──────────────────────────────

    override suspend fun addDogAndLinkToUser(dog: DogDto): Result<DogDto, dogError> {
        return try {
            val uid = currentUid()

            val draft = dog.copy(id = "", ownerId = uid)
            val ref = dogsCol.add(draft)
            val newId = ref.id
            val saved = draft.copy(id = newId)

            val userRef = usersCol.document(uid)
            val snap = userRef.get()
            val user = if (snap.exists) snap.data<UserDto>()
            else UserDto(email = auth.currentUser?.email ?: "", ownerName = auth.currentUser?.displayName ?: "", dogList = emptyList())

            val newList = (user.dogList ?: emptyList()).filter { it.id != newId } + saved
            userRef.set(user.copy(dogList = newList), merge = true)

            Result.Success(saved)
        } catch (e: Exception) {
            Result.Failure(dogError(e.message ?: "Add dog failed"))
        }
    }

    override suspend fun updateDogAndUser(dog: DogDto): Result<Unit, dogError> {
        return try {
            val uid = currentUid()
            val id = dog.id.ifBlank { return Result.Failure(dogError("Dog id missing")) }

            dogsCol.document(id).set(dog, merge = true)

            val userRef = usersCol.document(uid)
            val snap = userRef.get()
            if (snap.exists) {
                val user = snap.data<UserDto>()
                val updated = (user.dogList ?: emptyList()).map { if (it.id == id) dog else it }
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

            dogsCol.document(dogId).delete()

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
