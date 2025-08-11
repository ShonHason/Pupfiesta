// app/src/main/java/org/example/project/MainActivity.kt
package org.example.project

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import org.example.project.data.dogGardens.GoogleGardensRepo
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.dogGardens.DogGardensViewModel
import org.example.project.features.registration.UserViewModel
import org.example.project.presentation.screens.home.GardenScreen
import org.example.project.presentation.screens.landing.LandingScreen
import org.example.project.presentation.screens.login.LoginScreen
import org.example.project.presentation.screens.registration.RegistrationScreen
import org.example.project.utils.httpClient

class MainActivity : ComponentActivity() {

    private fun mapsApiKeyFromManifest(): String {
        val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Firebase SDK
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // Create shared repo once (not composable)
        val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository()
        val placesKey = mapsApiKeyFromManifest()

        setContent {
            val navController = rememberNavController()

            // Build the Google repo INSIDE a composable (or you can build it above without remember)
            val gardensRepo = remember {
                GoogleGardensRepo(
                    client = httpClient(),
                    apiKey = placesKey
                )
            }

            val userVm = remember { UserViewModel(firebaseRepo) }
            val gardenVm = remember { DogGardensViewModel(firebaseRepo, gardensRepo) }

            NavHost(navController = navController, startDestination = "landing") {
                composable("landing") {
                    LandingScreen(
                        onSignIn   = { navController.navigate("login") },
                        onRegister = { navController.navigate("register") }
                    )
                }

                composable("register") {
                    RegistrationScreen(
                        viewModel    = userVm,
                        onBack       = { navController.popBackStack() },
                        onRegistered = {
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true }
                            }
                        }
                    )
                }

                composable("login") {
                    LoginScreen(
                        viewModel         = userVm,
                        onBack            = { navController.popBackStack() },
                        onRecoverPassword = { /* TODO */ },
                        onSignIn          = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onGoogle          = { /* TODO */ },
                        onApple           = { /* TODO */ },
                        onRegister        = { navController.navigate("register") }
                    )
                }

                composable("home") {
                    GardenScreen(
                        viewModel = gardenVm,
                        onBack    = { navController.popBackStack() },
                        onScan    = { /* handle scan action */ }
                    )
                }
            }
        }
    }
}
