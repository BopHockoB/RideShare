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
import java.util.Calendar
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

    // Selected route
    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute

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

    // All routes
    private val _allRoutes = MutableStateFlow<List<Route>>(emptyList())
    val allRoutes: StateFlow<List<Route>> = _allRoutes

    // Set current user ID
    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
        loadUserTrips(userId)
    }

    // Get current user ID
    fun getCurrentUserId(): String? {
        return _currentUserId.value
    }

    // ====================
    // TRIP METHODS
    // ====================

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
     * Get a trip by ID - Flow version
     */
    fun getTripById(tripId: String): Flow<Trip?> {
        return tripRepository.getTripById(tripId)
    }

    /**
     * Get a trip by ID - returns a single value instead of Flow
     * This is better for one-time data loading
     */
    suspend fun getTripByIdOnce(tripId: String): Trip? {
        return try {
            tripRepository.getTripById(tripId).first()
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to load trip"
            null
        }
    }

    // ====================
    // ROUTE METHODS
    // ====================

    /**
     * Get all routes
     */
    fun loadAllRoutes() {
        viewModelScope.launch {
            try {
                routeRepository.allRoutes.collect { routes ->
                    _allRoutes.value = routes
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load routes"
            }
        }
    }

    /**
     * Get a route by ID - Flow version
     */
    fun getRouteById(routeId: String): Flow<Route?> {
        return routeRepository.getRouteById(routeId)
    }

    /**
     * Get a route by ID - returns a single value instead of Flow
     */
    suspend fun getRouteByIdOnce(routeId: String): Route? {
        return try {
            routeRepository.getRouteById(routeId).first()
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to load route"
            null
        }
    }

    /**
     * Search routes by location name
     */
    fun searchRoutes(query: String): Flow<List<Route>> {
        return routeRepository.searchRoutes(query)
    }

    /**
     * Get routes in a specific area
     */
    fun getRoutesInArea(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<Route>> {
        return routeRepository.getRoutesInArea(minLat, maxLat, minLng, maxLng)
    }

    /**
     * Get routes by start and end area
     */
    fun getRoutesByStartAndEndArea(
        startMinLat: Double, startMaxLat: Double,
        startMinLng: Double, startMaxLng: Double,
        endMinLat: Double, endMaxLat: Double,
        endMinLng: Double, endMaxLng: Double
    ): Flow<List<Route>> {
        return routeRepository.getRoutesByStartAndEndArea(
            startMinLat, startMaxLat, startMinLng, startMaxLng,
            endMinLat, endMaxLat, endMinLng, endMaxLng
        )
    }

    /**
     * Get route for a specific trip
     */
    suspend fun getRouteForTrip(trip: Trip): Route? {
        return try {
            getRouteByIdOnce(trip.routeId)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load route for trip: ${e.message}"
            null
        }
    }

    /**
     * Get route for a specific trip - Flow version
     */
    fun getRouteForTripFlow(trip: Trip): Flow<Route?> {
        return getRouteById(trip.routeId)
    }

    /**
     * Get trip with its route data combined
     */
    suspend fun getTripWithRoute(tripId: String): Pair<Trip?, Route?> {
        return try {
            val trip = getTripByIdOnce(tripId)
            val route = trip?.let { getRouteByIdOnce(it.routeId) }
            Pair(trip, route)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load trip with route: ${e.message}"
            Pair(null, null)
        }
    }

    /**
     * Get trip with its route data combined - Flow version
     */
    fun getTripWithRouteFlow(tripId: String): Flow<Pair<Trip?, Route?>> {
        return getTripById(tripId).flatMapLatest { trip ->
            if (trip != null) {
                getRouteById(trip.routeId).map { route ->
                    Pair(trip, route)
                }
            } else {
                flowOf(Pair(null, null))
            }
        }
    }

    /**
     * Create a new route
     */
    suspend fun createRoute(
        startLocationName: String,
        startAddress: String,
        startLatitude: Double,
        startLongitude: Double,
        endLocationName: String,
        endAddress: String,
        endLatitude: Double,
        endLongitude: Double
    ): String {
        return try {
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
            routeId
        } catch (e: Exception) {
            _errorMessage.value = "Failed to create route: ${e.message}"
            throw e
        }
    }

    /**
     * Update an existing route
     */
    suspend fun updateRoute(route: Route) {
        try {
            routeRepository.updateRoute(route)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update route: ${e.message}"
            throw e
        }
    }

    /**
     * Delete a route
     */
    suspend fun deleteRoute(routeId: String) {
        try {
            routeRepository.deleteRouteById(routeId)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to delete route: ${e.message}"
            throw e
        }
    }

    // ====================
    // TRIP CREATION AND MANAGEMENT
    // ====================

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
            val routeId = createRoute(
                startLocationName, startAddress, startLatitude, startLongitude,
                endLocationName, endAddress, endLatitude, endLongitude
            )

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

    // ====================
    // UTILITY METHODS
    // ====================

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
     * Search for trips based on location and filters
     */
    suspend fun searchTrips(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        departureDate: Long? = null,
        maxPrice: Double? = null,
        minSeats: Int = 1
    ): List<Trip> {
        return try {
            // Get routes in the area around start and end points
            val searchRadius = 0.05 // roughly 5km in degrees

            getRoutesByStartAndEndArea(
                startMinLat = startLatitude - searchRadius,
                startMaxLat = startLatitude + searchRadius,
                startMinLng = startLongitude - searchRadius,
                startMaxLng = startLongitude + searchRadius,
                endMinLat = endLatitude - searchRadius,
                endMaxLat = endLatitude + searchRadius,
                endMinLng = endLongitude - searchRadius,
                endMaxLng = endLongitude + searchRadius
            ).first().let { routes ->
                // Get trips for these routes
                val allTrips = mutableListOf<Trip>()
                routes.forEach { route ->
                    val tripsForRoute = tripRepository.getTripsByRouteId(route.routeId).first()
                    allTrips.addAll(tripsForRoute)
                }

                // Filter trips based on criteria
                allTrips.filter { trip ->
                    // Only scheduled trips with available seats
                    trip.status == "SCHEDULED" &&
                            trip.availableSeats >= minSeats &&

                            // Future trips only
                            trip.departureTime > System.currentTimeMillis() &&

                            // Price filter
                            (maxPrice == null || trip.price <= maxPrice) &&

                            // Date filter (if specified, match the day)
                            (departureDate == null ||
                                    isSameDay(trip.departureTime, departureDate))
                }.sortedBy { it.departureTime } // Sort by departure time
            }

        } catch (e: Exception) {
            _errorMessage.value = "Search failed: ${e.message}"
            emptyList()
        }
    }

    /**
     * Search trips with location names (simpler version)
     */
    suspend fun searchTripsByLocation(
        startLocation: String,
        endLocation: String,
        departureDate: Long? = null,
        maxPrice: Double? = null,
        minSeats: Int = 1
    ): List<Trip> {
        return try {
            // Search routes by location names
            val startRoutes = routeRepository.searchRoutes(startLocation).first()
            val endRoutes = routeRepository.searchRoutes(endLocation).first()

            // Find routes that match both start and end criteria
            val matchingRoutes = startRoutes.intersect(endRoutes.toSet())

            // Get trips for matching routes
            val allTrips = mutableListOf<Trip>()
            matchingRoutes.forEach { route ->
                val tripsForRoute = tripRepository.getTripsByRouteId(route.routeId).first()
                allTrips.addAll(tripsForRoute)
            }

            // Apply filters
            allTrips.filter { trip ->
                trip.status == "SCHEDULED" &&
                        trip.availableSeats >= minSeats &&
                        trip.departureTime > System.currentTimeMillis() &&
                        (maxPrice == null || trip.price <= maxPrice) &&
                        (departureDate == null || isSameDay(trip.departureTime, departureDate))
            }.sortedBy { it.departureTime }

        } catch (e: Exception) {
            _errorMessage.value = "Location search failed: ${e.message}"
            emptyList()
        }
    }

    /**
     * Get popular/recent routes for search suggestions
     */
    fun getPopularRoutes(): Flow<List<Route>> {
        return routeRepository.allRoutes.map { routes ->
            // Return most recent routes as "popular" ones
            // In a real app, you'd track usage statistics
            routes.sortedByDescending { it.createdAt }.take(10)
        }
    }

    /**
     * Check if two timestamps are on the same day
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getTripsByDriverId(driverId: String): Flow<List<Trip>> {
        return tripRepository.getTripsByDriverId(driverId)
    }
}