//package org.example.project.data
//
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class SignResponse(
//    val cloudName: String,
//    val apiKey: String,
//    val timestamp: Long,
//    val signature: String,
//    val signedParams: Map<String, String?> // e.g. folder/public_id/overwrite/timestamp (timestamp also a top-level field)
//)
//
//interface SignProvider {
//    suspend fun signUpload(
//        folder: String?,
//        publicId: String?,
//        overwrite: Boolean?
//    ): SignResponse
//}