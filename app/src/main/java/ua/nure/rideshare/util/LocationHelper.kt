package ua.nure.rideshare.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.*

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Helper class to manage location-related functionality
 */
class LocationHelper(private val context: Context) {

    // Constants
    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val FASTEST_INTERVAL = 5000L // 5 seconds
        private const val MAX_WAIT_TIME = 15000L // 15 seconds
    }

    // Private location client
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Private location callback
    private var locationCallback: LocationCallback? = null

    // Location state flow for observers
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    // Location updates state - FIXED: Use public property without asStateFlow()
    private val _isReceivingUpdates = MutableStateFlow(false)
    val isReceivingUpdates: StateFlow<Boolean> = _isReceivingUpdates

    // Last known location timestamp
    private val _lastUpdateTime = MutableStateFlow<Long?>(null)
    val lastUpdateTime: StateFlow<Long?> = _lastUpdateTime

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the last known location once
     * @param onSuccess Callback when location is successfully retrieved
     * @param onFailure Callback when location retrieval fails
     */
    fun getLastLocation(
        onSuccess: (LatLng) -> Unit = {},
        onFailure: (Exception?) -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            onFailure(SecurityException("Location permission not granted"))
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        _userLocation.value = userLatLng
                        _lastUpdateTime.value = System.currentTimeMillis()
                        onSuccess(userLatLng)
                    } ?: run {
                        Log.d(TAG, "Last location is null, requesting location updates")
                        // If last location is null, try requesting updates once
                        requestLocationUpdates(
                            onUpdate = { latLng ->
                                onSuccess(latLng)
                                // Stop updates after getting one location
                                stopLocationUpdates()
                            },
                            onFailure = onFailure
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting last location", e)
                    onFailure(e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting last location", e)
            onFailure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception when getting last location", e)
            onFailure(e)
        }
    }

    /**
     * Start receiving continuous location updates
     * @param onUpdate Callback when a new location update is received
     * @param onFailure Callback when location updates fail
     */
    fun requestLocationUpdates(
        onUpdate: (LatLng) -> Unit = {},
        onFailure: (Exception?) -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            onFailure(SecurityException("Location permission not granted"))
            return
        }

        try {
            val locationRequest = createLocationRequest()

            // Create a new location callback if needed
            if (locationCallback == null) {
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { location ->
                            val latLng = LatLng(location.latitude, location.longitude)
                            _userLocation.value = latLng
                            _lastUpdateTime.value = System.currentTimeMillis()
                            onUpdate(latLng)
                        }
                    }
                }
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                _isReceivingUpdates.value = true
                Log.d(TAG, "Location updates started")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error starting location updates", e)
                _isReceivingUpdates.value = false
                onFailure(e)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
            onFailure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception when requesting location updates", e)
            onFailure(e)
        }
    }

    /**
     * Stop receiving location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            _isReceivingUpdates.value = false
            Log.d(TAG, "Location updates stopped")
        }
    }

    /**
     * Create location request with appropriate settings
     */
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMaxUpdateDelayMillis(MAX_WAIT_TIME)
            .build()
    }

    /**
     * Clean up resources when no longer needed
     */
    fun cleanup() {
        stopLocationUpdates()
        locationCallback = null
    }
}