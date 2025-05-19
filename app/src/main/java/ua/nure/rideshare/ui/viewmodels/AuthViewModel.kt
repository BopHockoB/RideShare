package ua.nure.rideshare.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.User
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.repository.UserRepository
import javax.inject.Inject

/**
 * ViewModel for user authentication and profile management
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // State for login
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // State for current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // State for current profile
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    // Current user ID for easy access
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    // Event for one-time navigation
    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent: SharedFlow<AuthEvent> = _authEvent

    /**
     * Log in a user with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                // This is a simplified authentication flow
                // In a real app, you would hash the password and compare with stored hash
                val user = userRepository.getUserByEmail(email)

                if (user != null && user.passwordHash == password) { // In reality, you'd use a proper hash check
                    // Update login timestamp
                    userRepository.updateUserLogin(user.userId)

                    // Get user profile
                    val profile = userRepository.getProfileByEmail(email)

                    // Update state
                    _currentUser.value = user
                    _currentProfile.value = profile
                    _currentUserId.value = user.userId
                    _loginState.value = LoginState.Success

                    // Emit navigation event with user ID
                    _authEvent.emit(AuthEvent.NavigateToHome(user.userId))
                } else {
                    _loginState.value = LoginState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Register a new user
     */
    fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        gender: String,
        age: Int
    ) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                // Check if user already exists
                val existingUser = userRepository.getUserByEmail(email)
                if (existingUser != null) {
                    _loginState.value = LoginState.Error("User with this email already exists")
                    return@launch
                }

                // Create new user (in real app, hash the password properly)
                val userId = java.util.UUID.randomUUID().toString()
                val user = User(
                    userId = userId,
                    email = email,
                    passwordHash = password, // In reality, hash this password
                    isEmailVerified = false,
                    authProvider = "EMAIL",
                    accountStatus = "ACTIVE"
                )

                // Create profile
                val profile = Profile(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phoneNumber = phoneNumber,
                    gender = gender,
                    age = age
                )

                // Save to database
                userRepository.insertUser(user)
                userRepository.insertProfile(profile)

                // Update state
                _currentUser.value = user
                _currentProfile.value = profile
                _currentUserId.value = userId
                _loginState.value = LoginState.Success

                // Emit navigation event with user ID
                _authEvent.emit(AuthEvent.NavigateToHome(userId))

            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Check if user is logged in (e.g., from shared preferences or token storage)
     * This is a placeholder - you would implement actual token validation here
     */
    fun checkLoggedInUser() {
        // In a real app, you would check for tokens in secure storage
        // and validate them with your backend

        // For now, we'll just reset the state
        _loginState.value = LoginState.Idle
        _currentUser.value = null
        _currentProfile.value = null
        _currentUserId.value = null
    }

    /**
     * Log out the current user
     */
    fun logout() {
        viewModelScope.launch {
            // In a real app, invalidate tokens, etc.
            _currentUser.value = null
            _currentProfile.value = null
            _currentUserId.value = null
            _loginState.value = LoginState.Idle
            _authEvent.emit(AuthEvent.NavigateToLogin)
        }
    }
}

/**
 * Represents the state of the login process
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * Events for navigation
 */
sealed class AuthEvent {
    object NavigateToLogin : AuthEvent()
    data class NavigateToHome(val userId: String) : AuthEvent()
    object NavigateToRegister : AuthEvent()
}
