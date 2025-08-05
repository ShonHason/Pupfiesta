package org.example.project.presentation.screens.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import org.example.project.R
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LandingScreen(
    onSignIn: () -> Unit,
    onRegister: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB0D4F8)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                // these came from the Card before:
                .fillMaxWidth(0.9f)
                .aspectRatio(0.6f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Lottie Animation
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.pet_lovers)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B6FF7))
                ) {
                    Text("Sign In", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onRegister,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4B6FF7))
                ) {
                    Text("Register", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
