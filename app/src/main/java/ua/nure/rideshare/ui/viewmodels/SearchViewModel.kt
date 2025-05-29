package ua.nure.rideshare.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.data.repository.TripRepository
import ua.nure.rideshare.data.repository.RouteRepository
import ua.nure.rideshare.data.repository.UserRepository
import ua.nure.rideshare.data.repository.CarRepository
import javax.inject.Inject

/**
 * Data class representing a complete search result with all related information
 */
data class TripSearchResult(
    val trip: Trip,
    val route: Route,
    val driverProfile: Profile,
    val car: Car?
)

/**
 * ViewModel for trip search functionality
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val routeRepository: RouteRepository,
    private val userRepository: UserRepository,
    private val carRepository: CarRepository
) : ViewModel() {

    // Search results
    private val _searchResults = MutableStateFlow<List<TripSearchResult>>(emptyList())
    val searchResults: StateFlow<List<TripSearchResult>> = _searchResults

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Search filters
    private val _currentFilters = MutableStateFlow(SearchFilters())
    val currentFilters: StateFlow<SearchFilters> = _currentFilters

    /**
     * Search for trips based on criteria
     */
    fun searchTrips(
        fromQuery: String,
        toQuery: String,
        departureDate: Long? = null,
        passengerCount: Int = 1,
        maxPrice: Double? = null,
        minRating: Float? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                Log.d("SEARCH_VIEWMODEL", "Searching trips from '$fromQuery' to '$toQuery'")

                // Get all available trips
                tripRepository.getAvailableTrips(passengerCount).take(1).collect { trips ->
                    Log.d("SEARCH_VIEWMODEL", "Found ${trips.size} available trips")

                    if (trips.isEmpty()) {
                        _searchResults.value = emptyList()
                        _isLoading.value = false
                        return@collect
                    }

                    // For each trip, get the associated route, driver profile, and car
                    val searchResults = mutableListOf<TripSearchResult>()

                    for (trip in trips) {
                        try {
                            // Get route information
                            val route = getRouteById(trip.routeId)
                            if (route == null) {
                                Log.w("SEARCH_VIEWMODEL", "Route not found for trip ${trip.tripId}")
                                continue
                            }

                            // Check if route matches search criteria
                            if (!matchesLocationCriteria(route, fromQuery, toQuery)) {
                                Log.d("SEARCH_VIEWMODEL", "Route doesn't match criteria: ${route.startLocation} -> ${route.endLocation}")
                                continue
                            }

                            // Get driver profile
                            val driverProfile = getProfileById(trip.driverId)
                            if (driverProfile == null) {
                                Log.w("SEARCH_VIEWMODEL", "Driver profile not found for trip ${trip.tripId}")
                                continue
                            }

                            // Apply rating filter
                            if (minRating != null && driverProfile.rating < minRating) {
                                Log.d("SEARCH_VIEWMODEL", "Driver rating ${driverProfile.rating} below minimum $minRating")
                                continue
                            }

                            // Apply price filter
                            if (maxPrice != null && trip.price > maxPrice) {
                                Log.d("SEARCH_VIEWMODEL", "Trip price ${trip.price} above maximum $maxPrice")
                                continue
                            }

                            // Apply date filter
                            if (departureDate != null && !matchesDateCriteria(trip.departureTime, departureDate)) {
                                Log.d("SEARCH_VIEWMODEL", "Trip date doesn't match criteria")
                                continue
                            }

                            // Get car information (optional)
                            val car = trip.carId?.let { getCarById(it) }

                            // Create search result
                            val searchResult = TripSearchResult(
                                trip = trip,
                                route = route,
                                driverProfile = driverProfile,
                                car = car
                            )

                            searchResults.add(searchResult)
                            Log.d("SEARCH_VIEWMODEL", "Added search result: ${route.startLocation} -> ${route.endLocation}")

                        } catch (e: Exception) {
                            Log.e("SEARCH_VIEWMODEL", "Error processing trip ${trip.tripId}: ${e.message}", e)
                        }
                    }

                    // Sort results by departure time (earliest first)
                    val sortedResults = searchResults.sortedBy { it.trip.departureTime }

                    Log.d("SEARCH_VIEWMODEL", "Final search results: ${sortedResults.size} trips")
                    _searchResults.value = sortedResults
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Error searching trips: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to search trips"
                _isLoading.value = false
            }
        }
    }

    /**
     * Search trips by geographic area
     */
    fun searchTripsByArea(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        radiusKm: Double = 5.0,
        passengerCount: Int = 1
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                Log.d("SEARCH_VIEWMODEL", "Searching trips by area")

                // Calculate search bounds
                val latRange = radiusKm / 111.0 // Approximate km to degrees
                val lngRange = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(startLat)))

                val startMinLat = startLat - latRange
                val startMaxLat = startLat + latRange
                val startMinLng = startLng - lngRange
                val startMaxLng = startLng + lngRange

                val endMinLat = endLat - latRange
                val endMaxLat = endLat + latRange
                val endMinLng = endLng - lngRange
                val endMaxLng = endLng + lngRange

                // Search routes in the specified areas
                routeRepository.getRoutesByStartAndEndArea(
                    startMinLat, startMaxLat, startMinLng, startMaxLng,
                    endMinLat, endMaxLat, endMinLng, endMaxLng
                ).take(1).collect { routes ->
                    Log.d("SEARCH_VIEWMODEL", "Found ${routes.size} routes in area")

                    if (routes.isEmpty()) {
                        _searchResults.value = emptyList()
                        _isLoading.value = false
                        return@collect
                    }

                    // Get trips for these routes
                    val searchResults = mutableListOf<TripSearchResult>()

                    for (route in routes) {
                        try {
                            // Get trips for this route
                            tripRepository.getTripsByRouteId(route.routeId).take(1).collect { trips ->
                                for (trip in trips) {
                                    // Check if trip has enough seats and is available
                                    if (trip.availableSeats < passengerCount || trip.status != "SCHEDULED") {
                                        continue
                                    }

                                    // Get driver profile
                                    val driverProfile = getProfileById(trip.driverId)
                                    if (driverProfile == null) {
                                        Log.w("SEARCH_VIEWMODEL", "Driver profile not found for trip ${trip.tripId}")
                                        continue
                                    }

                                    // Get car information (optional)
                                    val car = trip.carId?.let { getCarById(it) }

                                    // Create search result
                                    val searchResult = TripSearchResult(
                                        trip = trip,
                                        route = route,
                                        driverProfile = driverProfile,
                                        car = car
                                    )

                                    searchResults.add(searchResult)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SEARCH_VIEWMODEL", "Error processing route ${route.routeId}: ${e.message}", e)
                        }
                    }

                    // Sort results by departure time
                    val sortedResults = searchResults.sortedBy { it.trip.departureTime }

                    Log.d("SEARCH_VIEWMODEL", "Area search results: ${sortedResults.size} trips")
                    _searchResults.value = sortedResults
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Error searching trips by area: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to search trips in area"
                _isLoading.value = false
            }
        }
    }

    /**
     * Get popular or recent trips
     */
    fun loadPopularTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                tripRepository.getAvailableTrips().take(1).collect { trips ->
                    // Take first 10 trips as "popular" (in a real app, this would be based on booking frequency)
                    val popularTrips = trips.take(10)

                    val searchResults = mutableListOf<TripSearchResult>()

                    for (trip in popularTrips) {
                        try {
                            val route = getRouteById(trip.routeId)
                            val driverProfile = route?.let { getProfileById(trip.driverId) }
                            val car = trip.carId?.let { getCarById(it) }

                            if (route != null && driverProfile != null) {
                                searchResults.add(
                                    TripSearchResult(
                                        trip = trip,
                                        route = route,
                                        driverProfile = driverProfile,
                                        car = car
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("SEARCH_VIEWMODEL", "Error loading popular trip ${trip.tripId}: ${e.message}", e)
                        }
                    }

                    _searchResults.value = searchResults.sortedBy { it.trip.departureTime }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Error loading popular trips: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to load popular trips"
                _isLoading.value = false
            }
        }
    }

    /**
     * Apply additional filters to current search results
     */
    fun applyFilters(filters: SearchFilters) {
        _currentFilters.value = filters

        val currentResults = _searchResults.value
        val filteredResults = currentResults.filter { result ->
            // Price filter
            (filters.maxPrice == null || result.trip.price <= filters.maxPrice) &&
                    // Rating filter
                    (filters.minRating == null || result.driverProfile.rating >= filters.minRating) &&
                    // Departure time filter
                    (filters.earliestDeparture == null || result.trip.departureTime >= filters.earliestDeparture) &&
                    (filters.latestDeparture == null || result.trip.departureTime <= filters.latestDeparture) &&
                    // Car amenities filter (if specified)
                    (filters.requiredAmenities == null ||
                            result.car?.amenities?.let { amenities ->
                                filters.requiredAmenities.all { required ->
                                    amenities.contains(required, ignoreCase = true)
                                }
                            } == true)
        }

        // Apply sorting
        val sortedResults = when (filters.sortBy) {
            SortOption.PRICE_LOW_TO_HIGH -> filteredResults.sortedBy { it.trip.price }
            SortOption.PRICE_HIGH_TO_LOW -> filteredResults.sortedByDescending { it.trip.price }
            SortOption.DEPARTURE_TIME -> filteredResults.sortedBy { it.trip.departureTime }
            SortOption.RATING -> filteredResults.sortedByDescending { it.driverProfile.rating }
            SortOption.DISTANCE -> filteredResults.sortedBy { it.route.distance }
            else -> filteredResults.sortedBy { it.trip.departureTime }
        }

        _searchResults.value = sortedResults
    }

    /**
     * Clear current search results
     */
    fun clearResults() {
        _searchResults.value = emptyList()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // Helper methods

    private suspend fun getRouteById(routeId: String): Route? {
        var route: Route? = null
        routeRepository.getRouteById(routeId).take(1).collect {
            route = it
        }
        return route
    }

    private suspend fun getProfileById(userId: String): Profile? {
        var profile: Profile? = null
        userRepository.getProfileById(userId).take(1).collect {
            profile = it
        }
        return profile
    }

    private suspend fun getCarById(carId: String): Car? {
        var car: Car? = null
        carRepository.getCarById(carId).take(1).collect {
            car = it
        }
        return car
    }

    private fun matchesLocationCriteria(route: Route, fromQuery: String, toQuery: String): Boolean {
        // Simple text matching - in a real app, this would use geocoding and fuzzy matching
        val fromMatches = fromQuery.isBlank() ||
                route.startLocation.contains(fromQuery, ignoreCase = true) ||
                route.startAddress.contains(fromQuery, ignoreCase = true) ||
                fromQuery.contains("current", ignoreCase = true) // Handle "Current Location"

        val toMatches = toQuery.isBlank() ||
                route.endLocation.contains(toQuery, ignoreCase = true) ||
                route.endAddress.contains(toQuery, ignoreCase = true)

        return fromMatches && toMatches
    }

    private fun matchesDateCriteria(tripDepartureTime: Long, searchDate: Long): Boolean {
        // Check if trip is on the same day as the search date
        val tripCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = tripDepartureTime
        }

        val searchCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = searchDate
        }

        return tripCalendar.get(java.util.Calendar.YEAR) == searchCalendar.get(java.util.Calendar.YEAR) &&
                tripCalendar.get(java.util.Calendar.DAY_OF_YEAR) == searchCalendar.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

/**
 * Data class for search filters
 */
data class SearchFilters(
    val maxPrice: Double? = null,
    val minRating: Float? = null,
    val earliestDeparture: Long? = null,
    val latestDeparture: Long? = null,
    val requiredAmenities: List<String>? = null,
    val sortBy: SortOption = SortOption.DEPARTURE_TIME
)

/**
 * Enum for sorting options
 */
enum class SortOption {
    DEPARTURE_TIME,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    RATING,
    DISTANCE
}