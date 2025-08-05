package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.presentation.screens.landing.LandingScreen
import org.example.project.presentation.screens.registration.RegistrationScreen
import com.google.firebase.FirebaseApp
import org.example.project.features.registration.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1️⃣ Init Firebase SDK
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        // 2️⃣ Create your repo + view-model
        val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository()
        setContent {
            // Use remember so we don’t re-create it on every recomposition
            val viewModel = remember { UserViewModel(firebaseRepo) }

            // 3️⃣ Show the registration screen
            RegistrationScreen(
                viewModel    = viewModel,
                onBack       = { finish() },           // simply close the Activity
                onRegistered = {
                    // once registered, swap in your LandingScreen
                    setContent {
                        LandingScreen(
                            onSignIn = TODO(),
                            onRegister = TODO()
                        )
                    }
                }
            )
        }
    }
}
