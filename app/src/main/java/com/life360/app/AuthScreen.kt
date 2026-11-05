
package com.life360.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

private const val SUPABASE_REDIRECT_URL =
    "https://shreyastapre50-ctrl.github.io/LiFE360/verification/"

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val green = Color(0xFF176B45)
    val textColor = Color(0xFF202820)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var registrationSubmitted by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        focusedLabelColor = green,
        unfocusedLabelColor = Color.DarkGray,
        focusedBorderColor = green,
        unfocusedBorderColor = Color.Gray,
        cursorColor = green
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF6F8F6))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "LiFE360",
            style = MaterialTheme.typography.headlineLarge,
            color = green,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Citizen Sustainability Intelligence Platform",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                message = ""
                registrationSubmitted = false
            },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                message = ""
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    },
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = green
                    )
                }
            },
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val enteredEmail = email.trim()
                val enteredPassword = password

                if (enteredEmail.isBlank() || enteredPassword.isBlank()) {
                    message = "Please enter your email and password."
                    return@Button
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(enteredEmail).matches()
                ) {
                    message = "Please enter a valid email address."
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    message = ""

                    try {
                        supabase.auth.signInWith(Email) {
                            this.email = enteredEmail
                            this.password = enteredPassword
                        }

                        onLoginSuccess()

                    } catch (e: Exception) {
                        message = e.message
                            ?: "Login failed. Please try again."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3478F6),
                contentColor = Color.White
            )
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val enteredEmail = email.trim()
                val enteredPassword = password

                if (enteredEmail.isBlank() || enteredPassword.isBlank()) {
                    message = "Enter your email and password to create an account."
                    return@OutlinedButton
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(enteredEmail).matches()
                ) {
                    message = "Please enter a valid email address."
                    return@OutlinedButton
                }

                if (enteredPassword.length < 6) {
                    message = "Password must contain at least 6 characters."
                    return@OutlinedButton
                }

                scope.launch {
                    isLoading = true
                    message = ""

                    try {
                        supabase.auth.signUpWith(
                            Email,
                            redirectUrl = SUPABASE_REDIRECT_URL
                        ) {
                            this.email = enteredEmail
                            this.password = enteredPassword
                        }

                        registrationSubmitted = true

                    } catch (e: Exception) {
                        message = e.message
                            ?: "Registration failed. Please try again."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = green
            )
        ) {
            Text("Create Account", color = green)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = green)

                Spacer(modifier = Modifier.width(12.dp))

                Text("Please wait...", color = textColor)
            }
        }

        if (registrationSubmitted) {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE5F5EB)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "📩 Check Your Email",
                        color = green,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "We have submitted your registration. Open the confirmation email sent to:",
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = email.trim(),
                        color = green,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "After confirming your email, return to LiFE360 and sign in using your registered email and password.",
                        color = textColor
                    )
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = Color(0xFFB3261E),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
