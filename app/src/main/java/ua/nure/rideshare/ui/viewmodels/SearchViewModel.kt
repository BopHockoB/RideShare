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
     * Initialize and load some sample data for testing
     */
    init {
        // Load some trips on initialization for testing
        loadAllAvailableTrips()
    }

    /**
     * Load all available trips (for testing purposes)
     */
    private fun loadAllAvailableTrips() {
        viewModelScope.launch {
            try {
                tripRepository.getAvailableTrips().collect { trips ->
                    Log.d("SEARCH_VIEWMODEL", "Available trips updated: ${trips.size} trips")
                }
            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Error loading available trips", e)
            }
        }
    }

    /**
     * Search for trips based on criteria - SIMPLIFIED VERSION
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
            _searchResults.value = emptyList() // Clear previous results

            try {
                Log.d("SEARCH_VIEWMODEL", "=== SEARCH DEBUG ===")
                Log.d("SEARCH_VIEWMODEL", "From: '$fromQuery'")
                Log.d("SEARCH_VIEWMODEL", "To: '$toQuery'")
                Log.d("SEARCH_VIEWMODEL", "Passengers: $passengerCount")

                // If both queries are empty, load all available trips
                if (fromQuery.isBlank() && toQuery.isBlank()) {
                    Log.d("SEARCH_VIEWMODEL", "Both queries empty, loading all trips")
                    loadPopularTrips()
                    return@launch
                }

                // Get all trips first
                val allTrips = tripRepository.getAvailableTrips().first()
                Log.d("SEARCH_VIEWMODEL", "Total trips in database: ${allTrips.size}")

                // Filter for available trips only
                val availableTrips = allTrips.filter {trip ->
                    trip.status == "SCHEDULED" &&
                            trip.availableSeats >= passengerCount &&
                            trip.departureTime > System.currentTimeMillis()
                }
                Log.d("SEARCH_VIEWMODEL", "Available trips: ${availableTrips.size}")

                // Build search results
                val searchResults = mutableListOf<TripSearchResult>()

                for (trip in availableTrips) {
                    try {
                        // Get route
                        val route = routeRepository.getRouteById(trip.routeId).first()
                        if (route == null) {
                            Log.w("SEARCH_VIEWMODEL", "No route found for trip ${trip.tripId}")
                            continue
                        }

                        Log.d("SEARCH_VIEWMODEL", "Route: ${route.startLocation} -> ${route.endLocation}")

                        // Apply location matching with more flexible criteria
                        val fromMatches = fromQuery.isBlank() ||
                                fromQuery.equals("Current Location", ignoreCase = true) ||
                                route.startLocation.contains(fromQuery, ignoreCase = true) ||
                                route.startAddress.contains(fromQuery, ignoreCase = true) ||
                                fromQuery.contains(route.startLocation, ignoreCase = true)

                        val toMatches = toQuery.isBlank() ||
                                route.endLocation.contains(toQuery, ignoreCase = true) ||
                                route.endAddress.contains(toQuery, ignoreCase = true) ||
                                toQuery.contains(route.endLocation, ignoreCase = true)

                        if (!fromMatches || !toMatches) {
                            Log.d("SEARCH_VIEWMODEL", "Location mismatch - From matches: $fromMatches, To matches: $toMatches")
                            continue
                        }

                        // Get driver profile
                        val driverProfile = userRepository.getProfileById(trip.driverId).first()
                        if (driverProfile == null) {
                            Log.w("SEARCH_VIEWMODEL", "No driver profile found for trip ${trip.tripId}")
                            continue
                        }

                        // Apply filters
                        if (maxPrice != null && trip.price > maxPrice) {
                            Log.d("SEARCH_VIEWMODEL", "Price filter failed: ${trip.price} > $maxPrice")
                            continue
                        }

                        if (minRating != null && driverProfile.rating < minRating) {
                            Log.d("SEARCH_VIEWMODEL", "Rating filter failed: ${driverProfile.rating} < $minRating")
                            continue
                        }

                        if (departureDate != null && !isSameDay(trip.departureTime, departureDate)) {
                            Log.d("SEARCH_VIEWMODEL", "Date filter failed")
                            continue
                        }

                        // Get car (optional)
                        val car = trip.carId?.let { carId ->
                            carRepository.getCarById(carId).first()
                        }

                        // Add to results
                        val searchResult = TripSearchResult(
                            trip = trip,
                            route = route,
                            driverProfile = driverProfile,
                            car = car
                        )
                        searchResults.add(searchResult)
                        Log.d("SEARCH_VIEWMODEL", "✅ Added trip: ${route.startLocation} -> ${route.endLocation}")

                    } catch (e: Exception) {
                        Log.e("SEARCH_VIEWMODEL", "Error processing trip ${trip.tripId}", e)
                    }
                }

                // Sort by departure time
                val sortedResults = searchResults.sortedBy { it.trip.departureTime }

                Log.d("SEARCH_VIEWMODEL", "=== SEARCH COMPLETE ===")
                Log.d("SEARCH_VIEWMODEL", "Found ${sortedResults.size} matching trips")

                _searchResults.value = sortedResults
                _isLoading.value = false

                // If no results found, show a helpful message
                if (sortedResults.isEmpty()) {
                    _errorMessage.value = "No trips found matching your criteria. Try adjusting your search."
                }

            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Search error", e)
                _errorMessage.value = "Search failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load popular/recent trips (simplified)
     */
    fun loadPopularTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                Log.d("SEARCH_VIEWMODEL", "Loading popular trips...")

                // Get all available trips
                val availableTrips = tripRepository.getAvailableTrips().first()
                Log.d("SEARCH_VIEWMODEL", "Found ${availableTrips.size} available trips")

                val searchResults = mutableListOf<TripSearchResult>()

                // Take up to 10 trips
                for (trip in availableTrips.take(10)) {
                    try {
                        // Get route
                        val route = routeRepository.getRouteById(trip.routeId).first()
                        if (route == null) continue

                        // Get driver profile
                        val driverProfile = userRepository.getProfileById(trip.driverId).first()
                        if (driverProfile == null) continue

                        // Get car (optional)
                        val car = trip.carId?.let { carId ->
                            carRepository.getCarById(carId).first()
                        }

                        searchResults.add(
                            TripSearchResult(
                                trip = trip,
                                route = route,
                                driverProfile = driverProfile,
                                car = car
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SEARCH_VIEWMODEL", "Error loading trip ${trip.tripId}", e)
                    }
                }

                val sortedResults = searchResults.sortedBy { it.trip.departureTime }
                Log.d("SEARCH_VIEWMODEL", "Loaded ${sortedResults.size} popular trips")

                _searchResults.value = sortedResults
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Error loading popular trips", e)
                _errorMessage.value = "Failed to load trips: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Search by coordinates (simplified)
     */
    fun searchTripsByCoordinates(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
        radiusKm: Double = 10.0,
        passengerCount: Int = 1
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                Log.d("SEARCH_VIEWMODEL", "Searching by coordinates...")
                Log.d("SEARCH_VIEWMODEL", "From: ($fromLat, $fromLng)")
                Log.d("SEARCH_VIEWMODEL", "To: ($toLat, $toLng)")
                Log.d("SEARCH_VIEWMODEL", "Radius: $radiusKm km")

                // Calculate search bounds
                val kmPerDegree = 111.0
                val latRange = radiusKm / kmPerDegree
                val lngRange = radiusKm / (kmPerDegree * kotlin.math.cos(Math.toRadians(fromLat)))

                // Get all routes first
                val allRoutes = routeRepository.allRoutes.first()
                Log.d("SEARCH_VIEWMODEL", "Total routes: ${allRoutes.size}")

                // Filter routes by location
                val matchingRoutes = allRoutes.filter { route ->
                    val startMatches = isWithinRadius(
                        route.startLatitude, route.startLongitude,
                        fromLat, fromLng, radiusKm
                    )
                    val endMatches = isWithinRadius(
                        route.endLatitude, route.endLongitude,
                        toLat, toLng, radiusKm
                    )
                    startMatches && endMatches
                }

                Log.d("SEARCH_VIEWMODEL", "Matching routes: ${matchingRoutes.size}")

                val searchResults = mutableListOf<TripSearchResult>()

                // Get trips for matching routes
                for (route in matchingRoutes) {
                    val trips = tripRepository.getTripsByRouteId(route.routeId).first()

                    for (trip in trips) {
                        if (trip.status != "SCHEDULED" ||
                            trip.availableSeats < passengerCount ||
                            trip.departureTime <= System.currentTimeMillis()) {
                            continue
                        }

                        try {
                            val driverProfile = userRepository.getProfileById(trip.driverId).first()
                            if (driverProfile == null) continue

                            val car = trip.carId?.let { carRepository.getCarById(it).first() }

                            searchResults.add(
                                TripSearchResult(
                                    trip = trip,
                                    route = route,
                                    driverProfile = driverProfile,
                                    car = car
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("SEARCH_VIEWMODEL", "Error processing trip ${trip.tripId}", e)
                        }
                    }
                }

                val sortedResults = searchResults.sortedBy { it.trip.departureTime }
                Log.d("SEARCH_VIEWMODEL", "Found ${sortedResults.size} trips by location")

                _searchResults.value = sortedResults
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("SEARCH_VIEWMODEL", "Error searching by coordinates", e)
                _errorMessage.value = "Location search failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Apply filters to current results
     */
    fun applyFilters(filters: SearchFilters) {
        _currentFilters.value = filters

        val currentResults = _searchResults.value
        val filteredResults = currentResults.filter { result ->
            (filters.maxPrice == null || result.trip.price <= filters.maxPrice) &&
                    (filters.minRating == null || result.driverProfile.rating >= filters.minRating) &&
                    (filters.earliestDeparture == null || result.trip.departureTime >= filters.earliestDeparture) &&
                    (filters.latestDeparture == null || result.trip.departureTime <= filters.latestDeparture)
        }

        // Apply sorting
        val sortedResults = when (filters.sortBy) {
            SortOption.PRICE_LOW_TO_HIGH -> filteredResults.sortedBy { it.trip.price }
            SortOption.PRICE_HIGH_TO_LOW -> filteredResults.sortedByDescending { it.trip.price }
            SortOption.DEPARTURE_TIME -> filteredResults.sortedBy { it.trip.departureTime }
            SortOption.RATING -> filteredResults.sortedByDescending { it.driverProfile.rating }
            SortOption.DISTANCE -> filteredResults.sortedBy { it.route.distance }
        }

        _searchResults.value = sortedResults
    }

    /**
     * Clear results
     */
    fun clearResults() {
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }

    /**
     * Clear error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // Helper functions

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun isWithinRadius(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double,
        radiusKm: Double
    ): Boolean {
        val distance = calculateDistance(lat1, lng1, lat2, lng2)
        return distance <= radiusKm
    }

    private fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
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