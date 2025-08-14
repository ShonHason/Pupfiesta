package org.example.project



import android.app.Application
import com.google.firebase.FirebaseApp
import org.example.project.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.example.project.utils.appContext  // from your androidMain getLocation actual

class MyLocationApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // expose a safe appContext for getLocation()
        appContext = applicationContext

        // start DI (shared Koin graph)
        initKoin {
            androidContext(this@MyLocationApp)
        }

        // optional: Firebase here instead of in Activity
        FirebaseApp.initializeApp(this)
        android.util.Log.d("GetLocation", "MyLocationApp.onCreate (appContext set)")
    }
}