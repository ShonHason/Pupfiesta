//package org.example.project.utils
//
//import io.ktor.client.HttpClient
//import io.ktor.client.call.body
//import io.ktor.client.request.forms.formData
//import io.ktor.client.request.forms.submitFormWithBinaryData
//import io.ktor.client.statement.bodyAsText
//import io.ktor.http.Headers
//import io.ktor.http.HttpHeaders
//import io.ktor.http.isSuccess
//import org.example.project.data.remote.dto.CloudinaryDto
//import org.example.project.data.sign.SignProvider
//import org.example.project.domain.models.UploadResult
//
//class CloudinaryService(
//    private val http: HttpClient,
//    private val signProvider: SignProvider,
//    private val defaultFolder: String = "kmp_uploads"
//) {
//    /**
//     * Signed image upload. NOTE: any param included here (folder/public_id/overwrite/…)
//     * must also be part of the server-side signature, otherwise Cloudinary rejects it.
//     */
//    suspend fun uploadImageSigned(
//        bytes: ByteArray,
//        fileName: String,
//        folder: String? = defaultFolder,
//        publicId: String? = null,
//        overwrite: Boolean? = true
//    ): UploadResult {
//        // 1) Ask platform SignProvider (Firebase Function) for a signature
//        val sign = signProvider.signUpload(folder, publicId, overwrite)
//
//        // 2) Build endpoint
//        val url = "https://api.cloudinary.com/v1_1/${sign.cloudName}/image/upload"
//
//        // 3) Send multipart form with EXACT signed params + file
//        val resp = http.submitFormWithBinaryData(
//            url = url,
//            formData = formData {
//                // required auth fields
//                append("api_key", sign.apiKey)
//                append("timestamp", sign.timestamp.toString())
//                append("signature", sign.signature)
//
//                // IMPORTANT: must send the same values that were signed server-side
//                sign.signedParams["folder"]?.let { append("folder", it) }
//                sign.signedParams["public_id"]?.let { append("public_id", it) }
//                sign.signedParams["overwrite"]?.let { append("overwrite", it) }
//
//                // the file
//                val contentType = guessContentType(bytes, fileName)
//                append(
//                    key = "file",
//                    value = bytes,
//                    headers = Headers.build {
//                        append(HttpHeaders.ContentType, contentType)
//                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
//                    }
//                )
//            }
//        )
//
//        if (!resp.status.isSuccess()) {
//            throw IllegalStateException("Cloudinary upload failed: ${resp.status} - ${resp.bodyAsText()}")
//        }
//
//        // 4) Parse Cloudinary response → domain model
//        val body: CloudinaryDto = resp.body()
//        return UploadResult(
//            url = body.secureUrl ?: "",
//            publicId = body.publicId ?: "",
//            width = body.width ?: 0,
//            height = body.height ?: 0,
//            bytes = body.bytes ?: 0
//        )
//    }
//
//    /**
//     * Very small content-type guesser for common image formats.
//     * Falls back to image/jpeg if unknown.
//     */
//    private fun guessContentType(bytes: ByteArray, fileName: String): String {
//        if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
//            return "image/jpeg"
//        }
//        if (bytes.size >= 8 &&
//            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() &&
//            bytes[3] == 0x47.toByte() && bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
//            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
//        ) {
//            return "image/png"
//        }
//        // WEBP: "RIFF....WEBP"
//        if (bytes.size >= 12 &&
//            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
//            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
//            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
//            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
//        ) {
//            return "image/webp"
//        }
//        // HEIC/HEIF (best-effort)
//        if (bytes.size >= 12 &&
//            bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() &&
//            bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()
//        ) {
//            // do not over-specify subtype; Cloudinary accepts image/heic or image/heif
//            return "image/heic"
//        }
//        // fallback
//        return if (fileName.endsWith(".png", true)) "image/png" else "image/jpeg"
//    }
//}