package ua.nure.rideshare.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    init {
        // Check for existing session on initialization
        checkLoggedInUser()
    }

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

                    // Save session to SharedPreferences
                    saveUserSession(user.userId, user.email)

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

                // Save session to SharedPreferences
                saveUserSession(userId, email)

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
     * Check if user is logged in and restore session
     */
    fun checkLoggedInUser() {
        viewModelScope.launch {
            try {
                val savedUserId = sharedPrefs.getString(KEY_USER_ID, null)
                val savedEmail = sharedPrefs.getString(KEY_USER_EMAIL, null)

                if (savedUserId != null && savedEmail != null) {
                    // Try to restore user session
                    val user = userRepository.getUserByEmail(savedEmail)
                    val profile = userRepository.getProfileByEmail(savedEmail)

                    if (user != null && user.userId == savedUserId) {
                        // Session is valid, restore state
                        _currentUser.value = user
                        _currentProfile.value = profile
                        _currentUserId.value = savedUserId
                        _loginState.value = LoginState.Success

                        android.util.Log.d("AUTH_VIEWMODEL", "Session restored for user: $savedUserId")
                        return@launch
                    } else {
                        // Session is invalid, clear it
                        clearUserSession()
                    }
                }

                // No valid session found, reset state
                _loginState.value = LoginState.Idle
                _currentUser.value = null
                _currentProfile.value = null
                _currentUserId.value = null

                android.util.Log.d("AUTH_VIEWMODEL", "No valid session found")
            } catch (e: Exception) {
                android.util.Log.e("AUTH_VIEWMODEL", "Error checking logged in user", e)
                clearUserSession()
                _loginState.value = LoginState.Idle
                _currentUser.value = null
                _currentProfile.value = null
                _currentUserId.value = null
            }
        }
    }

    /**
     * Log out the current user
     */
    fun logout() {
        viewModelScope.launch {
            // Clear session from SharedPreferences
            clearUserSession()

            // Reset state
            _currentUser.value = null
            _currentProfile.value = null
            _currentUserId.value = null
            _loginState.value = LoginState.Idle

            _authEvent.emit(AuthEvent.NavigateToLogin)

            android.util.Log.d("AUTH_VIEWMODEL", "User logged out")
        }
    }

    /**
     * Manually set current user ID (for debugging or special cases)
     */
    fun setCurrentUserId(userId: String) {
        viewModelScope.launch {
            try {
                // Load user data
                val user = userRepository.getUserById(userId).firstOrNull()
                if (user != null) {
                    val profile = userRepository.getProfileByEmail(user.email)

                    // Save session
                    saveUserSession(userId, user.email)

                    // Update state
                    _currentUser.value = user
                    _currentProfile.value = profile
                    _currentUserId.value = userId
                    _loginState.value = LoginState.Success

                    android.util.Log.d("AUTH_VIEWMODEL", "Current user set to: $userId")
                } else {
                    android.util.Log.w("AUTH_VIEWMODEL", "User not found for ID: $userId")
                }
            } catch (e: Exception) {
                android.util.Log.e("AUTH_VIEWMODEL", "Error setting current user", e)
            }
        }
    }

    /**
     * Get current user ID synchronously (for immediate access)
     */
    fun getCurrentUserIdSync(): String? {
        return _currentUserId.value ?: sharedPrefs.getString(KEY_USER_ID, null)
    }

    /**
     * Save user session to SharedPreferences
     */
    private fun saveUserSession(userId: String, email: String) {
        sharedPrefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .apply()

        android.util.Log.d("AUTH_VIEWMODEL", "User session saved: $userId")
    }

    /**
     * Clear user session from SharedPreferences
     */
    private fun clearUserSession() {
        sharedPrefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .apply()

        android.util.Log.d("AUTH_VIEWMODEL", "User session cleared")
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