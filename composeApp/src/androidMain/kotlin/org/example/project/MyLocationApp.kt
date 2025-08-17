package org.example.project

import android.app.Application
import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import org.example.project.shared.BuildConfig

class MyLocationApp : Application() {
    companion object {
        lateinit var ctx: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
        val config = hashMapOf(
            "cloud_name" to BuildConfig.CLOUD_NAME,
            "secure" to "true"
        )
        MediaManager.init(this, config)
        val cloudinary = MediaManager.get().cloudinary
        Log.d("MyLocationApp", "cloud_name=${cloudinary.config.cloudName} api_key=${cloudinary.config.apiKey}")
        check(cloudinary.config.apiKey.isNullOrBlank()) { "api_key must NOT be set for unsigned uploads" }
    }
}
