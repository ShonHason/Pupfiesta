// app/src/main/java/org/example/project/MainActivity.kt
package org.example.project

import PostAuthViewModel
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
import org.example.project.data.dogGardens.DogGardensViewModel
import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.di.startKoinIfNeeded
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.example.project.features.registration.UserViewModel
import org.example.project.presentation.screens.PostAuthScreen
import org.example.project.presentation.screens.addDog.AddDogScreen
import org.example.project.presentation.screens.home.GardenScreen
import org.example.project.presentation.screens.landing.LandingScreen
import org.example.project.presentation.screens.login.LoginScreen
import org.example.project.presentation.screens.profile.ProfileScreen
import org.example.project.presentation.screens.registration.RegistrationScreen
import org.example.project.utils.appContext
import org.koin.androidx.scope.scope
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private fun mapsApiKeyFromManifest(): String {
        val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext=applicationContext
        startKoinIfNeeded(applicationContext)
        FirebaseApp.initializeApp(this)


        // Init Firebase SDK


        enableEdgeToEdge()

        // Create shared repo once (not composable)
        val firebaseRepo: FirebaseRepository = RemoteFirebaseRepository()



        setContent {
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()


            // Build the Google repo INSIDE a composable (or you can build it above without remember)


            val userVm = remember { UserViewModel(firebaseRepo) }
            val gardenVm: DogGardensViewModel=koinInject()
            val postAuthVm: PostAuthViewModel = koinInject()


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
                            navController.navigate("post_auth") {
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
                            navController.navigate("post_auth") {
                                popUpTo("login") { inclusive = true }
                            }
                        },

                        onRegister        = { navController.navigate("register") }
                    )
                }

                composable("post_auth") {
                    PostAuthScreen(
                       viewModel = postAuthVm, // obtain via your DI / VM factory
                        onDone = {
                            navController.navigate("home") {
                                popUpTo("post_auth") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    GardenScreen(
                        onBack = { navController.popBackStack() },
                        onScan = { gardenVm.onScanClick() },
                        onGoProfile = {
                            navController.navigate("profile") {
                                launchSingleTop = true
                            }
                        },

                        onLogout = {
                            scope.launch {
                                // Sign out and clear any cached user state
                                firebaseRepo.logout()
                                userVm.invalidateUserCache()
                                // Navigate to your landing/login screen
                                navController.navigate("landing") {
                                    popUpTo(0) { inclusive = true }  // clear back stack
                                }
                            }
                        },
                    )
                }
                composable("addDog") {
                    AddDogScreen(
                        viewModel = userVm,
                        onBack = { navController.popBackStack() },
                        onDone = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        viewModel = userVm,
                        onBack = { navController.popBackStack() },
                        onGoYard = {

                            navController.navigate("home") {
                                launchSingleTop = true
                                popUpTo("home") { inclusive = false }
                            }
                        },
                        onAddDog =  { navController.navigate("addDog") }
                    )
                }
            }
        }
    }
}
