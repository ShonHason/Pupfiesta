// file: CloudinaryUploader.kt
package org.example.project

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.ByteArrayOutputStream

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"
    private const val UPLOAD_PRESET = "pupfiesta_unsigned"
    private const val FOLDER = "pupfiesta/dogs"

    fun upload(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        try {
            val publicId = "dog_${System.currentTimeMillis()}"
            MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", FOLDER)
                .option("public_id", publicId)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        val url = resultData["secure_url"] as? String
                        Log.d(TAG, "upload success: $url")
                        onResult(url)
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Cloudinary upload error: ${error.description}")
                        onResult(null)
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Cloudinary upload rescheduled: ${error.description}")
                        onResult(null)
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "upload failed", e)
            onResult(null)
        }
    }

    fun upload(context: Context, bitmap: Bitmap, onResult: (String?) -> Unit) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()
            stream.close()
            val publicId = "dog_${System.currentTimeMillis()}"
            MediaManager.get().upload(bytes)
                .unsigned(UPLOAD_PRESET)
                .option("folder", FOLDER)
                .option("public_id", publicId)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        val url = resultData["secure_url"] as? String
                        Log.d(TAG, "upload success: $url")
                        onResult(url)
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Cloudinary upload error: ${error.description}")
                        onResult(null)
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Cloudinary upload rescheduled: ${error.description}")
                        onResult(null)
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "upload failed", e)
            onResult(null)
        }
    }
}
