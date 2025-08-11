// utils/extension/MappingExtensions.kt
package org.example.project.utils.extension

import org.example.project.data.remote.dto.DogDto
import org.example.project.data.remote.dto.UserDto
import org.example.project.domain.models.Dog
import org.example.project.domain.models.User
import org.example.project.enum.Gender

/** ------ Firestore DTO → Domain ------ **/

/** Map a DogDto into your domain Dog */
fun DogDto.toDomain(): Dog = Dog(
    id             = this.id,
    dogName        = this.name,
    dogBreed       = this.breed,
    // if your DTO’s `isMale` is true → Gender.MALE, else FEMALE
    dogGender      = if (this.isMale) Gender.MALE else Gender.FEMALE,
    dogPictureUrl  = this.imgUrl,
    isNeutered     = this.isNeutered,
    dogWeight      = this.weight.toInt(),      // DTO weight is Double, domain is Int kg
    isFriendly     = this.isFriendly
)

/** Map a UserDto + Firebase UID → your domain User */
fun UserDto.toDomain(uid: String): User = User(
    id        = uid,
    email     = this.email,
    ownerName = this.name,
    dogList   = this.dogList.map { it.toDomain() }
)


/** ------ Domain → Firestore DTO ------ **/

/**
 * Build a DogDto from your domain Dog.
 * @param ownerId the UID of the user who owns this dog
 */
fun Dog.toDto(ownerId: String): DogDto = DogDto(
    id         = this.id,
    name       = this.dogName,
    breed      = this.dogBreed,
    weight     = this.dogWeight.toInt(),
    imgUrl     = this.dogPictureUrl ?: "",
    isFriendly = this.isFriendly,
    isMale     = (this.dogGender == Gender.MALE),
    isNeutered = this.isNeutered,
    ownerId    = ownerId
)

/** Convert your domain User → Firestore UserDto (wiring ownerId into each dog) */
fun User.toDto(): UserDto = UserDto(
    email   = this.email,
    name    = this.ownerName,
    dogList = this.dogList.map { it.toDto(this.id) }
)
