    // shared/src/commonMain/kotlin/org/example/project/data/firebase/RemoteFirebaseRepository.kt
    package org.example.project.data.firebase

    import dev.gitlive.firebase.Firebase
    import dev.gitlive.firebase.auth.FirebaseUser
    import dev.gitlive.firebase.auth.auth
    import dev.gitlive.firebase.firestore.firestore
    import org.example.project.data.local.Result
    import org.example.project.data.remote.dto.DogDto
    import org.example.project.data.remote.dto.UserDto
    import org.example.project.domain.models.AuthError
    import org.example.project.domain.models.DogGarden
    import org.example.project.domain.models.User
    import org.example.project.platformLogger
    import org.example.project.utils.extension.toDto
    import kotlin.math.*

    class RemoteFirebaseRepository : FirebaseRepository {

        private val auth = Firebase.auth
        private val firestore = Firebase.firestore

        // ——— User ———
        override suspend fun userLogin(email: String, password: String): Result<Unit, AuthError> = try {
            val user = auth.signInWithEmailAndPassword(email, password)
            platformLogger("PUP", "userLogin → uid=${user.user?.uid}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError("Login failed: ${e.message}"))
        }

        override suspend fun userRegistration(
            email: String,
            password: String,
            name: String,
            dogs: List<DogDto>
        ): Result<Unit, AuthError> {
            platformLogger("PUP", "userRegistration → email=$email, name=$name, dogs=$dogs")
            return try {
                val authResult = auth.createUserWithEmailAndPassword(email, password)
                val fbUser = authResult.user ?: return Result.Failure(AuthError("No UID returned"))
                fbUser.updateProfile(displayName = name)
                val uid = fbUser.uid

                // Save each dog under /Dogs
                val createdDogs = mutableListOf<DogDto>()
                for (dog in dogs) {
                    val dogWithOwner = dog.copy(ownerId = uid)
                    val ref = firestore.collection("Dogs").add(dogWithOwner)
                    firestore.collection("Dogs")
                        .document(ref.id)
                        .set(dogWithOwner.copy(id = ref.id), merge = true)
                    createdDogs += dogWithOwner.copy(id = ref.id)
                }

                // Write Users/{uid}
                val userDto = UserDto(email = email, name = name, dogList = createdDogs)
                firestore.collection("Users").document(uid).set(userDto)

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(AuthError("Registration failed: ${e.message}"))
            }
        }

        override suspend fun getCurrentUser(): Result<FirebaseUser, AuthError> =
            auth.currentUser?.let { Result.Success(it) }
                ?: Result.Failure(AuthError("No signed-in user"))

        override suspend fun updateUser(user: User): Result<Unit, AuthError> {
            val fbUser = auth.currentUser ?: return Result.Failure(AuthError("No user signed in"))
            return try {
                if (fbUser.displayName != user.ownerName) fbUser.updateProfile(displayName = user.ownerName)
                if (fbUser.email != user.email) fbUser.updateEmail(user.email)

                val uid = fbUser.uid
                val userRef = Firebase.firestore.collection("Users").document(uid)
                val existingDto: UserDto = userRef.get().data()

                val oldDogs = existingDto.dogList
                val newDto = user.toDto()
                val newDogs = newDto.dogList

                val needsDogUpdate = oldDogs.size != newDogs.size ||
                        oldDogs.zip(newDogs).any { (o, n) ->
                            o.name != n.name ||
                                    o.breed != n.breed ||
                                    o.weight != n.weight ||
                                    o.imgUrl != n.imgUrl ||
                                    o.isFriendly != n.isFriendly ||
                                    o.isMale != n.isMale ||
                                    o.isNeutered != n.isNeutered
                        }

                val payload = mutableMapOf<String, Any>("email" to user.email, "name" to user.ownerName)
                if (needsDogUpdate) payload["dogList"] = newDogs
                userRef.set(payload, merge = true)

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(AuthError("Update failed: ${e.message}"))
            }
        }

        override suspend fun logout(): Result<Unit, AuthError> = try {
            auth.signOut(); Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AuthError("Logout failed: ${e.message}")) }

        // ——— Dog gardens ———

        override suspend fun saveDogGardens(gardens: List<DogGarden>): Result<Unit, AuthError> = try {
            val col = firestore.collection("DogGardens")
            for (g in gardens) {
                if (g.id.isNotBlank()) {
                    col.document(g.id).set(g, merge = true)
                } else {
                    val ref = col.add(g)
                    col.document(ref.id).set(g.copy(id = ref.id), merge = true)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AuthError("Save gardens failed: ${e.message}"))
        }

        override suspend fun getDogGardens(): Result<List<DogGarden>, AuthError> = try {
            val snapshot = firestore.collection("DogGardens").get()
            val gardens = snapshot.documents.mapNotNull { it.data<DogGarden>() }
            Result.Success(gardens)
        } catch (e: Exception) {
            Result.Failure(AuthError("Load gardens failed: ${e.message}"))
        }

        override suspend fun getDogGardensNear(
            latitude: Double,
            longitude: Double,
            radiusMeters: Int
        ): Result<List<DogGarden>, AuthError> = try {
            // Load all, then filter by distance (Haversine)
            val all = (getDogGardens() as? Result.Success)?.data ?: emptyList()

            val filtered = all.filter { g ->
                val glat = g.location.latitude
                val glon = g.location.longitude
                distanceMeters(latitude, longitude, glat, glon) <= radiusMeters
            }

            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Failure(AuthError("Load nearby gardens failed: ${e.message}"))
        }

        // Haversine
        private fun toRad(d: Double) = d * PI / 180.0
        private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = toRad(lat2 - lat1)
            val dLon = toRad(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) + cos(toRad(lat1)) * cos(toRad(lat2)) * sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return 6_371_000.0 * c
        }
    }
