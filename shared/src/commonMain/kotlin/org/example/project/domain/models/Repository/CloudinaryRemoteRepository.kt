//package org.example.project.domain.models.Repository
//
//import io.ktor.client.plugins.ClientRequestException
//import io.ktor.client.plugins.ServerResponseException
//import io.ktor.client.statement.bodyAsText
//import org.example.project.data.local.Result
//import org.example.project.domain.models.UploadResult
//import org.example.project.domain.models.dogError
//import org.example.project.utils.CloudinaryService
//
//class CloudinaryRemoteRepository(
//    private val cloudinary: CloudinaryService
//): CloudinaryRepository {
//    override suspend fun uploadDogPhoto(
//        bytes: ByteArray,
//        folder: String,
//        publicId: String?,
//        overwrite: Boolean
//    ): Result<UploadResult, dogError> =
//        try {
//            val fileName = (publicId ?: "dog_${kotlin.system.getTimeMillis()}") + ".jpg"
//            val resp = cloudinary.uploadImageSigned(bytes, fileName, folder, publicId, overwrite)
//            Result.Success(resp)
//        } catch (e: ClientRequestException) {
//            Result.Failure(dogError("Upload failed (client ${e.response.status.value}): ${e.response.bodyAsText()}"))
//        } catch (e: ServerResponseException) {
//            Result.Failure(dogError("Upload failed (server ${e.response.status.value}): ${e.response.bodyAsText()}"))
//        }
//         catch (e: Throwable) {
//            Result.Failure(dogError(e.message ?: "Unexpected upload error"))
//        }
//}
