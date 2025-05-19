package ua.nure.rideshare.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import ua.nure.rideshare.ui.viewmodels.LoginState
import ua.nure.rideshare.ui.viewmodels.AuthEvent
import ua.nure.rideshare.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit, // Updated to receive userId
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Collect login state
    val loginState by viewModel.loginState.collectAsState()

    // Collect one-time events
    LaunchedEffect(key1 = true) {
        viewModel.authEvent.collectLatest { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onLoginSuccess(event.userId)
                is AuthEvent.NavigateToRegister -> onNavigateToRegister()
                else -> { /* Ignore other events */ }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            IconButton(onClick = { /* Handle back if needed */ }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Back",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Log in with your email or phone number",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp)
        )

        // Password field with visibility toggle
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(8.dp)
        )

        // Error message if any
        when (val state = loginState) {
            is LoginState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            else -> {}
        }

        // Login button
        Button(
            onClick = {
                viewModel.login(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = email.isNotEmpty() && password.isNotEmpty() && loginState != LoginState.Loading,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A16B) // Green color from the image
            )
        ) {
            if (loginState == LoginState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Log In", fontSize = 16.sp)
            }
        }

        // Divider with "or"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color = Color.LightGray
            )
            Text(
                text = "or",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray
            )
            Divider(
                modifier = Modifier.weight(1f),
                color = Color.LightGray
            )
        }

        // Google login button
        OutlinedButton(
            onClick = { /* Handle Google login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Black
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("G", modifier = Modifier.padding(end = 8.dp))
                Text("Log in with Google")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Facebook login button
        OutlinedButton(
            onClick = { /* Handle Facebook login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF1877F2) // Facebook blue
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("f", modifier = Modifier.padding(end = 8.dp))
                Text("Log in with Facebook")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Register prompt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account? ",
                color = Color.Gray
            )
            Text(
                text = "Sign up",
                color = Color(0xFF00A16B), // Green color from the image
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}