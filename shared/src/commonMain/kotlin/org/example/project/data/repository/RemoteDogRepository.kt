package org.example.project.data.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.example.project.data.local.Error
import org.example.project.data.local.Result
import org.example.project.data.remote.dto.DogDto
import org.example.project.domain.models.AuthError
import org.example.project.domain.models.Dog
import org.example.project.domain.models.dbError
import org.example.project.domain.models.dogError
import org.example.project.domain.repository.DogRepository
import org.example.project.utils.extension.toDomain

class RemoteDogRepository : DogRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    override suspend fun getOwnerDogs(): Result<List<Dog>, Error> {
        val currentUser = auth.currentUser
            ?: return Result.Failure(dogError("User not authenticated"))

        return try {
            val snapshot = firestore
                .collection("Dogs")
                .document(currentUser.uid)
                .collection("dogs")
                .get()

            // Deserialize each document → DogDto → domain Dog
            val dogs = snapshot.documents
                .mapNotNull { it.data<DogDto>() }
                .map { it.toDomain() }

            Result.Success(dogs)
        } catch (e: Exception) {
            Result.Failure(dbError("Failed to fetch dogs: ${e.message ?: "Unknown error"}"))
        }
    }

    override suspend fun getDogById(dogId: String): Result<Dog, Error> {
        val currentUser = auth.currentUser
            ?: return Result.Failure(AuthError("User not authenticated"))

        return try {
            // Look up /Dogs/{uid}/dogs/{dogId}
            val snapshot = firestore
                .collection("Dogs")
                .document(currentUser.uid)
                .collection("dogs")
                .document(dogId)
                .get()

            // Deserialize & map
            val dto: DogDto = snapshot.data()
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Failure(dbError("Failed to fetch dog: ${e.message ?: "Unknown error"}"))
        }
    }
}
