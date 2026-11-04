
package com.life360.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LiFE360",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Citizen Sustainability Intelligence Platform",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "Please enter email and password."
                    return@Button
                }

                val enteredEmail = email.trim()
                val enteredPassword = password

                scope.launch {
                    isLoading = true
                    message = ""

                    try {
                        supabase.auth.signInWith(Email) {
                            this.email = enteredEmail
                            this.password = enteredPassword
                        }

                        message = "Login successful!"
                        onLoginSuccess()

                    } catch (e: Exception) {
                        message = e.message ?: "Login failed."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "Please enter email and password."
                    return@OutlinedButton
                }

                val enteredEmail = email.trim()
                val enteredPassword = password

                scope.launch {
                    isLoading = true
                    message = ""

                    try {
                        supabase.auth.signUpWith(Email) {
                            this.email = enteredEmail
                            this.password = enteredPassword
                        }

                        message =
                            "Registration submitted. Check your email for confirmation."

                    } catch (e: Exception) {
                        message = e.message ?: "Registration failed."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
