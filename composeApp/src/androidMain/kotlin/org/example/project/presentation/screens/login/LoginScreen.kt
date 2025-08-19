// file: LoginScreen.kt
package org.example.project.presentation.screens.login


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import org.example.project.R
import org.example.project.data.user.UserEvent
import org.example.project.data.user.UserFormData
import org.example.project.data.user.UserState
import org.example.project.features.registration.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onRecoverPassword: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    val loginState by viewModel.userState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is UserState.Loaded) {
            onSignIn()
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8F1C4), Color(0xFFB0D4F8))
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "PupFiesta",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.height(40.dp))

            val data = (loginState as? UserState.Initial)?.data ?: UserFormData()

            OutlinedTextField(
                value = data.email,
                onValueChange = { viewModel.onEvent(UserEvent.EmailChanged(it)) },
                placeholder = { Text("Enter Email") },
                singleLine = true,
                trailingIcon = {
                    if (data.email.isNotBlank()) {
                        IconButton(onClick = { viewModel.onEvent(UserEvent.EmailChanged("")) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = data.password,
                onValueChange = { viewModel.onEvent(UserEvent.PasswordChanged(it)) },
                placeholder = { Text("Enter Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle Password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Recover Password?",
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onRecoverPassword),
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(32.dp))

            Box {
                Button(
                    onClick = { viewModel.onEvent(UserEvent.OnSignIn) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = loginState !is UserState.Loading
                ) {
                    Text("Sign In")
                }

                if (loginState is UserState.Loading) {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.paw_dog_loader)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )

                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (loginState is UserState.Error) {
                Text(
                    text = (loginState as UserState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Black)) {
                        append("if you don’t have an account you can ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF3B5BFF), fontWeight = FontWeight.Bold)) {
                        append("Register here")
                    }
                },
                modifier = Modifier.clickable(onClick = onRegister),
                fontSize = 14.sp
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(36.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}
