package ua.nure.rideshare.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import ua.nure.rideshare.ui.viewmodels.AuthEvent
import ua.nure.rideshare.ui.viewmodels.AuthViewModel
import ua.nure.rideshare.ui.viewmodels.LoginState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var age by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) }

    // Collect login state
    val loginState by viewModel.loginState.collectAsState()

    // Scroll state for form
    val scrollState = rememberScrollState()

    // Collect one-time events
    LaunchedEffect(key1 = true) {
        viewModel.authEvent.collectLatest { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onRegisterSuccess(event.userId)
                is AuthEvent.NavigateToLogin -> onNavigateToLogin()
                else -> { /* Ignore other events */ }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            IconButton(onClick = {
                if (currentStep == 1) {
                    onNavigateToLogin()
                } else {
                    currentStep = 1
                }
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Back",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (currentStep) {
            1 -> {
                // Step 1: Email and password
                Text(
                    text = "Sign up with your email",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )

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
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Error messages
                if (loginState is LoginState.Error) {
                    Text(
                        text = (loginState as LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Next button
                Button(
                    onClick = {
                        // Validate email and password
                        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                            // You would use ViewModel to handle this in a real app
                            return@Button
                        }

                        if (password != confirmPassword) {
                            // You would use ViewModel to handle this in a real app
                            return@Button
                        }

                        // Proceed to next step
                        currentStep = 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A16B)
                    )
                ) {
                    Text("Next", fontSize = 16.sp)
                }

                // Login link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color.Gray
                    )
                    Text(
                        text = "Sign in",
                        color = Color(0xFF00A16B),
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }

            2 -> {
                // Step 2: Profile details
                Text(
                    text = "Add profile details",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Tell us about yourself",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // First Name field
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Last Name field
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Phone number field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Age field
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Gender selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gender:",
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Row {
                        RadioButton(
                            selected = gender == "MALE",
                            onClick = { gender = "MALE" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF00A16B)
                            )
                        )
                        Text(
                            text = "Male",
                            modifier = Modifier
                                .clickable { gender = "MALE" }
                                .padding(start = 4.dp, end = 16.dp)
                        )

                        RadioButton(
                            selected = gender == "FEMALE",
                            onClick = { gender = "FEMALE" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF00A16B)
                            )
                        )
                        Text(
                            text = "Female",
                            modifier = Modifier
                                .clickable { gender = "FEMALE" }
                                .padding(start = 4.dp)
                        )
                    }
                }

                // Error messages
                if (loginState is LoginState.Error) {
                    Text(
                        text = (loginState as LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Register button
                Button(
                    onClick = {
                        // Validate fields
                        if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() || age.isEmpty()) {
                            // You would use ViewModel to handle this in a real app
                            return@Button
                        }

                        // Try to parse age
                        val ageInt = try {
                            age.toInt()
                        } catch (e: NumberFormatException) {
                            // Handle invalid age
                            return@Button
                        }

                        // Register user
                        viewModel.register(
                            email = email,
                            password = password,
                            firstName = firstName,
                            lastName = lastName,
                            phoneNumber = phoneNumber,
                            gender = gender,
                            age = ageInt
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = loginState != LoginState.Loading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A16B)
                    )
                ) {
                    if (loginState == LoginState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Register", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

