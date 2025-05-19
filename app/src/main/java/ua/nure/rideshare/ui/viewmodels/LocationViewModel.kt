package ua.nure.rideshare.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.nure.rideshare.utils.LocationHelper

/**
 * ViewModel to handle location data and expose it to the UI
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    // Location helper
    private val locationHelper = LocationHelper(application.applicationContext)

    // Location permission state
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted

    // Forward the location from the helper - FIXED: Direct reference to StateFlow properties
    val userLocation = locationHelper.userLocation

    // Location updates state - FIXED: Direct reference to StateFlow
    val isReceivingUpdates = locationHelper.isReceivingUpdates

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Clear any error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Update the location permission status
     */
    fun updateLocationPermissionStatus(granted: Boolean) {
        _locationPermissionGranted.value = granted
        if (granted) {
            // If permission is now granted, immediately try to get location
            getLastLocation()
        }
    }

    /**
     * Get the last known location
     */
    fun getLastLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            locationHelper.getLastLocation(
                onSuccess = {
                    _isLoading.value = false
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _errorMessage.value = e?.message ?: "Failed to get location"
                }
            )
        }
    }

    /**
     * Start receiving location updates
     */
    fun startLocationUpdates() {
        if (!locationHelper.hasLocationPermission()) {
            _errorMessage.value = "Location permission not granted"
            return
        }

        viewModelScope.launch {
            locationHelper.requestLocationUpdates(
                onFailure = { e ->
                    _errorMessage.value = e?.message ?: "Failed to start location updates"
                }
            )
        }
    }

    /**
     * Stop receiving location updates
     */
    fun stopLocationUpdates() {
        locationHelper.stopLocationUpdates()
    }

    /**
     * Check if we have location permissions
     */
    fun hasLocationPermission(): Boolean {
        return locationHelper.hasLocationPermission()
    }

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        locationHelper.cleanup()
    }
}