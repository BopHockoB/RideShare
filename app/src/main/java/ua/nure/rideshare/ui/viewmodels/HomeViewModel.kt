package ua.nure.rideshare.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.data.model.TripBooking
import ua.nure.rideshare.data.repository.UserRepository
import ua.nure.rideshare.data.repository.RouteRepository
import ua.nure.rideshare.data.repository.TripRepository
import ua.nure.rideshare.data.repository.CarRepository
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for home screen and trip interactions
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository,
    private val tripRepository: TripRepository,
    private val carRepository: CarRepository
) : ViewModel() {

    // Current user profile
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    // User's active cars
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    val userCars: StateFlow<List<Car>> = _userCars

    // Available trips
    private val _availableTrips = MutableStateFlow<List<Trip>>(emptyList())
    val availableTrips: StateFlow<List<Trip>> = _availableTrips

    // Trip creation state
    private val _tripCreationState = MutableStateFlow<TripCreationState>(TripCreationState.Idle)
    val tripCreationState: StateFlow<TripCreationState> = _tripCreationState

    // Nearby locations state
    private val _nearbyLocationsState = MutableStateFlow<NearbyLocationsState>(NearbyLocationsState.Idle)
    val nearbyLocationsState: StateFlow<NearbyLocationsState> = _nearbyLocationsState

    /**
     * Initialize the ViewModel with user data
     */
    fun initialize(userId: String) {
        viewModelScope.launch {
            // Load user profile
            userRepository.getProfileById(userId).collect { profile ->
                _currentProfile.value = profile

                // Load user's cars if profile exists
                profile?.let {
                    loadUserCars(it.userId)
                }
            }

            // Load available trips
            loadAvailableTrips()
        }
    }

    /**
     * Load user's cars
     */
    private fun loadUserCars(userId: String) {
        viewModelScope.launch {
            carRepository.getActiveCarsByOwnerId(userId).collect { cars ->
                _userCars.value = cars
            }
        }
    }

    /**
     * Load available trips
     */
    private fun loadAvailableTrips() {
        viewModelScope.launch {
            tripRepository.getAvailableTrips().collect { trips ->
                _availableTrips.value = trips
            }
        }
    }

    /**
     * Search for trips by location
     */
    fun searchTrips(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ) {
        viewModelScope.launch {
            _nearbyLocationsState.value = NearbyLocationsState.Loading

            try {
                // Create an area around the start point (roughly 5km radius)
                val startLatDelta = 0.045 // approximately 5km in latitude
                val startLngDelta = 0.045 / Math.cos(Math.toRadians(startLatitude))

                // Create an area around the end point
                val endLatDelta = 0.045
                val endLngDelta = 0.045 / Math.cos(Math.toRadians(endLatitude))

                // Search for routes in this area
                routeRepository.getRoutesByStartAndEndArea(
                    startMinLat = startLatitude - startLatDelta,
                    startMaxLat = startLatitude + startLatDelta,
                    startMinLng = startLongitude - startLngDelta,
                    startMaxLng = startLongitude + startLngDelta,
                    endMinLat = endLatitude - endLatDelta,
                    endMaxLat = endLatitude + endLatDelta,
                    endMinLng = endLongitude - endLngDelta,
                    endMaxLng = endLongitude + endLngDelta
                ).collect { routes ->
                    // Get trips for these routes
                    val tripList = mutableListOf<Trip>()
                    routes.forEach { route ->
                        tripRepository.getTripsByRouteId(route.routeId).collect { trips ->
                            tripList.addAll(trips.filter { it.availableSeats > 0 && it.status == "SCHEDULED" })
                        }
                    }

                    _nearbyLocationsState.value = NearbyLocationsState.Success(tripList)
                }
            } catch (e: Exception) {
                _nearbyLocationsState.value = NearbyLocationsState.Error(e.message ?: "Error searching for trips")
            }
        }
    }

    /**
     * Create a new trip as a driver
     */
    fun createTrip(
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
    ) {
        viewModelScope.launch {
            _tripCreationState.value = TripCreationState.Loading

            try {
                val userId = _currentProfile.value?.userId ?: throw IllegalStateException("User not logged in")

                // First, create the route
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
                    distance = calculateDistance(
                        startLatitude, startLongitude,
                        endLatitude, endLongitude
                    ),
                    duration = estimateDuration(
                        startLatitude, startLongitude,
                        endLatitude, endLongitude
                    ),
                    polyline = null // Would generate this with Google Maps API in a real app
                )

                routeRepository.insertRoute(route)

                // Then, create the trip
                val trip = Trip(
                    tripId = UUID.randomUUID().toString(),
                    driverId = userId,
                    carId = carId,
                    routeId = routeId,
                    departureTime = departureTime,
                    price = price,
                    availableSeats = availableSeats,
                    status = "SCHEDULED",
                    notes = notes
                )

                tripRepository.insertTrip(trip)

                // Increment trip count for the user
                userRepository.incrementTripsCount(userId)

                _tripCreationState.value = TripCreationState.Success(trip)

            } catch (e: Exception) {
                _tripCreationState.value = TripCreationState.Error(e.message ?: "Error creating trip")
            }
        }
    }

    /**
     * Book a trip as a passenger
     */
    fun bookTrip(
        tripId: String,
        seats: Int,
        pickupLocation: String? = null,
        pickupLatitude: Double? = null,
        pickupLongitude: Double? = null,
        dropoffLocation: String? = null,
        dropoffLatitude: Double? = null,
        dropoffLongitude: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val userId = _currentProfile.value?.userId ?: throw IllegalStateException("User not logged in")

                // Create booking
                val booking = TripBooking(
                    bookingId = UUID.randomUUID().toString(),
                    tripId = tripId,
                    passengerId = userId,
                    seats = seats,
                    pickupLocation = pickupLocation,
                    pickupLatitude = pickupLatitude,
                    pickupLongitude = pickupLongitude,
                    dropoffLocation = dropoffLocation,
                    dropoffLatitude = dropoffLatitude,
                    dropoffLongitude = dropoffLongitude,
                    status = "PENDING"
                )

                tripRepository.bookTrip(booking)

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Calculate the distance between two points
     * This is a simplified calculation - in a real app, you'd use Google Maps API
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

        return (earthRadius * c).toFloat()
    }

    /**
     * Estimate travel duration in minutes
     * This is a simplified calculation - in a real app, you'd use Google Maps API
     */
    private fun estimateDuration(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Int {
        // Simple estimate: 1.5 minutes per km, plus 5 minutes for stop/start
        val distance = calculateDistance(startLat, startLng, endLat, endLng)
        return (distance * 1.5 + 5).toInt()
    }
}

/**
 * State for trip creation
 */
sealed class TripCreationState {
    object Idle : TripCreationState()
    object Loading : TripCreationState()
    data class Success(val trip: Trip) : TripCreationState()
    data class Error(val message: String) : TripCreationState()
}

/**
 * State for nearby locations search
 */
sealed class NearbyLocationsState {
    object Idle : NearbyLocationsState()
    object Loading : NearbyLocationsState()
    data class Success(val trips: List<Trip>) : NearbyLocationsState()
    data class Error(val message: String) : NearbyLocationsState()
}
