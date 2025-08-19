package org.example.project.presentation.screens

import PostAuthViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import org.example.project.R

import org.example.project.features.postAuth.PostAuthState

import org.example.project.utils.Location

@Composable
fun PostAuthScreen(
    viewModel: PostAuthViewModel,
    onDone: () -> Unit,
    fallbackCenter: Location? = null
) {
    val state by viewModel.state.collectAsState()

    // Kick off once
    LaunchedEffect(Unit) {
        viewModel.run(
            radiusMeters = 11_000,
            locationTimeoutMs = 6_000,
            fallbackCenter = fallbackCenter
        )
    }

    // Auto-continue when finished (success or error)
    LaunchedEffect(state) {
        if (state is PostAuthState.Success || state is PostAuthState.Error) {
            delay(700)
            viewModel.reset()
            onDone()
        }
    }

    // Reuse the same Lottie as LandingScreen (R.raw.pet_lovers)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.pet_lovers))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier.size(240.dp)
            )
            Spacer(Modifier.height(16.dp))

            val message = when (val s = state) {
                is PostAuthState.Running -> s.message
                is PostAuthState.Success -> "Saved ${s.savedCount} dog parks!"
                is PostAuthState.Error   -> "Setup error: ${s.message}"
                PostAuthState.Idle       -> "Preparing…"
            }
            Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)

            if (state is PostAuthState.Error) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.reset(); onDone() }) { Text("Continue") }
            }
        }
    }
}