//package org.example.project.domain.models.Repository
//
//import org.example.project.domain.models.dogError
//
//import org.example.project.data.local.Result
//import org.example.project.domain.models.UploadResult
//
//interface CloudinaryRepository {
//    suspend fun uploadDogPhoto(
//       bytes: ByteArray,
//       folder:String,
//       publicId: String?,
//         overwrite: Boolean
//    ): Result<UploadResult, dogError>
//}