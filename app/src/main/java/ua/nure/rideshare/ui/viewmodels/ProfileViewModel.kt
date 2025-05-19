package ua.nure.rideshare.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.repository.UserRepository
import javax.inject.Inject

/**
 * ViewModel for profile management
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Profile state
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    // Edit state
    private val _editState = MutableStateFlow<EditState>(EditState.Idle)
    val editState: StateFlow<EditState> = _editState

    // Current profile
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile

    // User ID (would normally come from auth service)
    private var currentUserId: String? = null

    // One-time events
    private val _profileEvent = MutableSharedFlow<ProfileEvent>()
    val profileEvent: SharedFlow<ProfileEvent> = _profileEvent

    /**
     * Load profile for given user ID
     */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            currentUserId = userId
            _profileState.value = ProfileState.Loading

            try {
                userRepository.getProfileById(userId).collect { profile ->
                    _profile.value = profile
                    _profileState.value = if (profile != null) {
                        ProfileState.Success(profile)
                    } else {
                        ProfileState.Error("Profile not found")
                    }
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    /**
     * Update profile
     */
    fun updateProfile(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        gender: String,
        age: Int,
        bio: String?,
        drivingExperience: Int?
    ) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch

            _editState.value = EditState.Saving

            try {
                // Get current profile
                val currentProfile = _profile.value ?: throw IllegalStateException("Profile not loaded")

                // Create updated profile
                val updatedProfile = currentProfile.copy(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber,
                    gender = gender,
                    age = age,
                    bio = bio,
                    drivingExperience = drivingExperience,
                    updatedAt = System.currentTimeMillis()
                )

                // Save to database
                userRepository.updateProfile(updatedProfile)

                // Update state
                _profile.value = updatedProfile
                _profileState.value = ProfileState.Success(updatedProfile)
                _editState.value = EditState.Success

                // Emit event
                _profileEvent.emit(ProfileEvent.ProfileUpdated)

            } catch (e: Exception) {
                _editState.value = EditState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    /**
     * Log out user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                // Clear any local user data if needed
                currentUserId = null
                _profile.value = null
                _profileState.value = ProfileState.Initial

                // Emit logout event
                _profileEvent.emit(ProfileEvent.Logout)
            } catch (e: Exception) {
                // Handle any error during logout
                _profileEvent.emit(ProfileEvent.Error(e.message ?: "Logout failed"))
            }
        }
    }
}

/**
 * State of profile loading
 */
sealed class ProfileState {
    object Initial : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: Profile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

/**
 * State of profile editing
 */
sealed class EditState {
    object Idle : EditState()
    object Saving : EditState()
    object Success : EditState()
    data class Error(val message: String) : EditState()
}

/**
 * Events emitted by ProfileViewModel
 */
sealed class ProfileEvent {
    object ProfileUpdated : ProfileEvent()
    object Logout : ProfileEvent()
    data class Error(val message: String) : ProfileEvent()
}
