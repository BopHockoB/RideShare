package ua.nure.rideshare.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.repository.RouteRepository
import ua.nure.rideshare.data.repository.TripRepository
import ua.nure.rideshare.data.repository.UserRepository
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for ride/trip management
 */
@HiltViewModel
class RideViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    // Current user ID
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    // Selected trip
    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip

    // Trip creation state
    private val _tripCreationState = MutableStateFlow<TripCreationState>(TripCreationState.Idle)
    val tripCreationState: StateFlow<TripCreationState> = _tripCreationState

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Available trips
    private val _availableTrips = MutableStateFlow<List<Trip>>(emptyList())
    val availableTrips: StateFlow<List<Trip>> = _availableTrips

    // User trips (as driver)
    private val _userTrips = MutableStateFlow<List<Trip>>(emptyList())
    val userTrips: StateFlow<List<Trip>> = _userTrips

    // Set current user ID
    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
        loadUserTrips(userId)
    }

    // Get current user ID
    fun getCurrentUserId(): String? {
        return _currentUserId.value
    }

    /**
     * Load all available trips
     */
    fun loadAvailableTrips() {
        viewModelScope.launch {
            tripRepository.getAvailableTrips().collect { trips ->
                _availableTrips.value = trips
            }
        }
    }

    /**
     * Load trips created by a specific user (as driver)
     */
    fun loadUserTrips(userId: String) {
        viewModelScope.launch {
            tripRepository.getTripsByDriverId(userId).collect { trips ->
                _userTrips.value = trips
            }
        }
    }

    /**
     * Get a trip by ID
     */
    suspend fun getTripById(tripId: String): Trip? {
        var trip: Trip? = null
        tripRepository.getTripById(tripId).collect {
            trip = it
        }
        return trip
    }

    /**
     * Create a new trip/ride
     * @return The ID of the created trip
     */
    suspend fun createTrip(
        userId: String,
        startLocationName: String,
        startAddress: String,
        startLatitude: Double,
        startLongitude: Double,
        endLocationName: String,
        endAddress: String,
        endLatitude: Double,
        endLongitude: Double,
        departureTime: Long,
        price: Double,
        availableSeats: Int,
        carId: String,
        notes: String?
    ): String {
        _tripCreationState.value = TripCreationState.Loading

        try {
            // Create route first
            val routeId = UUID.randomUUID().toString()
            val route = Route(
                routeId = routeId,
                startLocation = startLocationName,
                startAddress = startAddress,
                startLatitude = startLatitude,
                startLongitude = startLongitude,
                endLocation = endLocationName,
                endAddress = endAddress,
                endLatitude = endLatitude,
                endLongitude = endLongitude,
                distance = calculateDistance(startLatitude, startLongitude, endLatitude, endLongitude),
                duration = estimateDuration(startLatitude, startLongitude, endLatitude, endLongitude),
                polyline = null // Would generate this with Google Maps API in a real app
            )

            routeRepository.insertRoute(route)

            // Now create the trip
            val tripId = UUID.randomUUID().toString()
            val trip = Trip(
                tripId = tripId,
                driverId = userId,
                carId = carId,
                routeId = routeId,
                departureTime = departureTime,
                price = price,
                availableSeats = availableSeats,
                status = "SCHEDULED", // Default status for new trips
                notes = notes
            )

            tripRepository.insertTrip(trip)

            // Increment trip count for the user
            userRepository.incrementTripsCount(userId)

            _tripCreationState.value = TripCreationState.Success(trip)

            return tripId
        } catch (e: Exception) {
            _tripCreationState.value = TripCreationState.Error(e.message ?: "Unknown error")
            _errorMessage.value = e.message ?: "Failed to create trip"
            throw e
        }
    }

    /**
     * Book a trip as a passenger
     */
    suspend fun bookTrip(
        tripId: String,
        seats: Int = 1,
        pickupLocation: String? = null,
        pickupLatitude: Double? = null,
        pickupLongitude: Double? = null,
        dropoffLocation: String? = null,
        dropoffLatitude: Double? = null,
        dropoffLongitude: Double? = null
    ): String {
        try {
            val userId = _currentUserId.value ?: throw IllegalStateException("User not logged in")

            // Create a booking object with a unique ID
            val bookingId = UUID.randomUUID().toString()
            val booking = ua.nure.rideshare.data.model.TripBooking(
                bookingId = bookingId,
                tripId = tripId,
                passengerId = userId,
                seats = seats,
                pickupLocation = pickupLocation,
                pickupLatitude = pickupLatitude,
                pickupLongitude = pickupLongitude,
                dropoffLocation = dropoffLocation,
                dropoffLatitude = dropoffLatitude,
                dropoffLongitude = dropoffLongitude,
                status = "PENDING" // Initial status
            )

            // Use the repository to book the trip
            tripRepository.bookTrip(booking)

            return bookingId
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to book trip"
            throw e
        }
    }

    /**
     * Cancel a trip as a driver
     */
    suspend fun cancelTrip(tripId: String) {
        try {
            tripRepository.updateTripStatus(tripId, "CANCELLED")
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to cancel trip"
            throw e
        }
    }

    /**
     * Update a trip's status
     */
    suspend fun updateTripStatus(tripId: String, status: String) {
        try {
            tripRepository.updateTripStatus(tripId, status)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to update trip status"
            throw e
        }
    }

    /**
     * Calculate distance between two points in kilometers
     */
    private fun calculateDistance(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val earthRadius = 6371f // Radius of the earth in km

        val latDistance = Math.toRadians(endLat - startLat)
        val lngDistance = Math.toRadians(endLng - startLng)

        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return ((earthRadius * c).toFloat())
    }

    /**
     * Estimate duration in minutes
     */
    private fun estimateDuration(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Int {
        // Simple estimate: approximate 1.5 minutes per km (40 km/h average speed)
        // plus 5 minutes fixed time for start/stop
        val distance = calculateDistance(startLat, startLng, endLat, endLng)
        return (distance * 1.5 + 5).toInt()
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
