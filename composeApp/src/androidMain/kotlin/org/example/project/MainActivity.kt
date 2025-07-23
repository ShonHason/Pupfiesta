package org.example.project

import LandingScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Simply supply two lambdas – they can be empty for now:
            LandingScreen(
                onSignIn   = { /* TODO: navigate to SignIn */ },
                onRegister = { /* TODO: navigate to Register */ }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}