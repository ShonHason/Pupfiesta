// androidMain/src/main/kotlin/org/example/project/MyLocationApp.kt
package org.example.project

import android.app.Application
import android.content.Context

class MyLocationApp : Application() {
    companion object {
        // lateinit so you can grab it from anywhere after onCreate()
        lateinit var ctx: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
    }
}
