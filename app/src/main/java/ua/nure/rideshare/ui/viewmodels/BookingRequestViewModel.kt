package ua.nure.rideshare.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.TripBooking
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.repository.TripRepository
import ua.nure.rideshare.data.repository.UserRepository
import ua.nure.rideshare.data.repository.RouteRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Data class representing a booking request with all necessary information
 */
data class BookingRequestData(
    val booking: TripBooking,
    val trip: Trip,
    val route: Route,
    val passengerProfile: Profile
)

/**
 * Data class for creating a new booking request
 */
data class CreateBookingRequest(
    val tripId: String,
    val passengerId: String,
    val seats: Int,
    val pickupLocation: String? = null,
    val pickupLatitude: Double? = null,
    val pickupLongitude: Double? = null,
    val dropoffLocation: String? = null,
    val dropoffLatitude: Double? = null,
    val dropoffLongitude: Double? = null,
    val message: String? = null
)

/**
 * ViewModel for managing booking requests
 */
@HiltViewModel
class BookingRequestViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BookingRequestVM"
    }

    // Current user ID
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    // Incoming booking requests (for drivers)
    private val _incomingBookingRequests = MutableStateFlow<List<BookingRequestData>>(emptyList())
    val incomingBookingRequests: StateFlow<List<BookingRequestData>> = _incomingBookingRequests

    // User's booking requests (for passengers)
    private val _userBookingRequests = MutableStateFlow<List<BookingRequestData>>(emptyList())
    val userBookingRequests: StateFlow<List<BookingRequestData>> = _userBookingRequests

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    // Error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Success events
    private val _bookingEvent = MutableSharedFlow<BookingEvent>()
    val bookingEvent: SharedFlow<BookingEvent> = _bookingEvent

    /**
     * Set the current user ID and load their booking data
     */
    fun setCurrentUserId(userId: String) {
        Log.d(TAG, "Setting current user ID: $userId")
        _currentUserId.value = userId
        loadUserBookingRequests(userId)
        loadIncomingBookingRequests(userId)
    }

    /**
     * Create a new booking request
     */
    fun createBookingRequest(request: CreateBookingRequest) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            try {
                Log.d(TAG, "Creating booking request for trip ${request.tripId} by passenger ${request.passengerId}")

                // Check if user already has a booking for this trip
                val existingBooking = tripRepository.getBookingByTripAndPassenger(
                    request.tripId,
                    request.passengerId
                )

                if (existingBooking != null) {
                    Log.w(TAG, "User already has booking: ${existingBooking.bookingId} with status ${existingBooking.status}")
                    _errorMessage.value = "You already have a booking request for this trip"
                    _isSubmitting.value = false
                    return@launch
                }

                // Get trip details to validate
                val trip = tripRepository.getTripById(request.tripId).first()
                if (trip == null) {
                    Log.e(TAG, "Trip not found: ${request.tripId}")
                    _errorMessage.value = "Trip not found"
                    _isSubmitting.value = false
                    return@launch
                }

                Log.d(TAG, "Trip found - Driver: ${trip.driverId}, Available seats: ${trip.availableSeats}, Status: ${trip.status}")

                // Validate trip availability
                if (trip.availableSeats < request.seats) {
                    Log.w(TAG, "Not enough seats. Requested: ${request.seats}, Available: ${trip.availableSeats}")
                    _errorMessage.value = "Not enough seats available"
                    _isSubmitting.value = false
                    return@launch
                }

                if (trip.status != "SCHEDULED") {
                    Log.w(TAG, "Trip not available for booking. Status: ${trip.status}")
                    _errorMessage.value = "This trip is no longer available for booking"
                    _isSubmitting.value = false
                    return@launch
                }

                // Prevent driver from booking their own trip
                if (trip.driverId == request.passengerId) {
                    Log.w(TAG, "Driver attempting to book own trip")
                    _errorMessage.value = "You cannot book your own trip"
                    _isSubmitting.value = false
                    return@launch
                }

                // Create the booking
                val booking = TripBooking(
                    bookingId = UUID.randomUUID().toString(),
                    tripId = request.tripId,
                    passengerId = request.passengerId,
                    seats = request.seats,
                    pickupLocation = request.pickupLocation,
                    pickupLatitude = request.pickupLatitude,
                    pickupLongitude = request.pickupLongitude,
                    dropoffLocation = request.dropoffLocation,
                    dropoffLatitude = request.dropoffLatitude,
                    dropoffLongitude = request.dropoffLongitude,
                    status = "PENDING",
                    paymentStatus = "PENDING",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                Log.d(TAG, "Inserting booking: ${booking.bookingId}")

                // Insert the booking (this will automatically decrease available seats)
                tripRepository.insertBooking(booking)

                Log.d(TAG, "✅ Booking request created successfully: ${booking.bookingId}")

                // Emit success event
                _bookingEvent.emit(BookingEvent.BookingCreated(booking))

                // Reload data for both passenger and driver
                loadUserBookingRequests(request.passengerId)

                // Also refresh driver's incoming requests if they're logged in
                _currentUserId.value?.let { currentUserId ->
                    if (currentUserId != request.passengerId) {
                        loadIncomingBookingRequests(currentUserId)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating booking request: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to create booking request"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /**
     * Load incoming booking requests for a driver - FIXED VERSION
     */
    private fun loadIncomingBookingRequests(driverId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚗 Loading incoming booking requests for driver: $driverId")

                // Get all trips created by this driver
                tripRepository.getTripsByDriverId(driverId).collect { trips ->
                    Log.d(TAG, "Driver has ${trips.size} trips")

                    if (trips.isEmpty()) {
                        Log.d(TAG, "No trips found for driver $driverId")
                        _incomingBookingRequests.value = emptyList()
                        return@collect
                    }

                    val incomingRequests = mutableListOf<BookingRequestData>()

                    // Process each trip
                    for (trip in trips) {
                        Log.d(TAG, "Processing trip: ${trip.tripId}")

                        try {
                            // Get all bookings for this trip
                            tripRepository.getBookingsByTripId(trip.tripId).take(1).collect { bookings ->
                                Log.d(TAG, "Trip ${trip.tripId} has ${bookings.size} bookings")

                                for (booking in bookings) {
                                    Log.d(TAG, "Booking ${booking.bookingId}: status=${booking.status}, passenger=${booking.passengerId}")

                                    // Include pending, approved, and recently created requests
                                    if (booking.status in listOf("PENDING", "APPROVED")) {
                                        try {
                                            // Get route information
                                            val route = routeRepository.getRouteById(trip.routeId).first()
                                            // Get passenger profile
                                            val passengerProfile = userRepository.getProfileById(booking.passengerId).first()

                                            if (route != null && passengerProfile != null) {
                                                val requestData = BookingRequestData(
                                                    booking = booking,
                                                    trip = trip,
                                                    route = route,
                                                    passengerProfile = passengerProfile
                                                )
                                                incomingRequests.add(requestData)
                                                Log.d(TAG, "✅ Added booking request: ${booking.bookingId} from ${passengerProfile.firstName}")
                                            } else {
                                                Log.w(TAG, "❌ Missing data - Route: ${route != null}, Profile: ${passengerProfile != null}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Error loading booking data for ${booking.bookingId}: ${e.message}", e)
                                        }
                                    } else {
                                        Log.d(TAG, "Skipping booking ${booking.bookingId} with status ${booking.status}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error loading bookings for trip ${trip.tripId}: ${e.message}", e)
                        }
                    }

                    // Sort by creation date (newest first)
                    val sortedRequests = incomingRequests.sortedByDescending { it.booking.createdAt }
                    _incomingBookingRequests.value = sortedRequests

                    Log.d(TAG, "🎉 Final result: ${sortedRequests.size} incoming booking requests for driver $driverId")
                    sortedRequests.forEach { request ->
                        Log.d(TAG, "  - ${request.passengerProfile.firstName} ${request.passengerProfile.lastName} (${request.booking.status})")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading incoming requests for driver $driverId: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to load incoming requests"
            }
        }
    }

    /**
     * Load booking requests made by a passenger - ENHANCED VERSION
     */
    private fun loadUserBookingRequests(passengerId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👤 Loading booking requests for passenger: $passengerId")

                tripRepository.getBookingsByPassengerId(passengerId).collect { bookings ->
                    Log.d(TAG, "Passenger has ${bookings.size} bookings")

                    val userRequests = mutableListOf<BookingRequestData>()

                    for (booking in bookings) {
                        Log.d(TAG, "Processing booking: ${booking.bookingId} for trip ${booking.tripId}")

                        try {
                            // Get trip information
                            val trip = tripRepository.getTripById(booking.tripId).first()
                            if (trip != null) {
                                // Get route information
                                val route = routeRepository.getRouteById(trip.routeId).first()
                                // Get passenger profile (self)
                                val passengerProfile = userRepository.getProfileById(passengerId).first()

                                if (route != null && passengerProfile != null) {
                                    userRequests.add(
                                        BookingRequestData(
                                            booking = booking,
                                            trip = trip,
                                            route = route,
                                            passengerProfile = passengerProfile
                                        )
                                    )
                                    Log.d(TAG, "✅ Added user booking: ${booking.bookingId}")
                                } else {
                                    Log.w(TAG, "❌ Missing data for booking ${booking.bookingId}")
                                }
                            } else {
                                Log.w(TAG, "❌ Trip not found for booking ${booking.bookingId}: ${booking.tripId}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error loading user booking data for ${booking.bookingId}: ${e.message}", e)
                        }
                    }

                    // Sort by creation date (newest first)
                    val sortedRequests = userRequests.sortedByDescending { it.booking.createdAt }
                    _userBookingRequests.value = sortedRequests

                    Log.d(TAG, "✅ Loaded ${sortedRequests.size} user booking requests for passenger $passengerId")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading user requests for passenger $passengerId: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to load your booking requests"
            }
        }
    }

    /**
     * Accept a booking request (for drivers)
     */
    fun acceptBookingRequest(bookingId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "✅ Accepting booking request: $bookingId")

                tripRepository.updateBookingStatus(bookingId, "APPROVED")

                _bookingEvent.emit(BookingEvent.BookingApproved(bookingId))

                // Reload incoming requests
                _currentUserId.value?.let { userId ->
                    loadIncomingBookingRequests(userId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error accepting booking: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to accept booking"
            }
        }
    }

    /**
     * Reject a booking request (for drivers)
     */
    fun rejectBookingRequest(bookingId: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "❌ Rejecting booking request: $bookingId")

                // Get the booking details to restore seats
                val booking = tripRepository.getBookingById(bookingId).first()
                if (booking != null) {
                    // Update status to rejected
                    tripRepository.updateBookingStatus(bookingId, "REJECTED")

                    // Restore the seats to the trip (since booking was rejected)
                    tripRepository.increaseAvailableSeats(booking.tripId, booking.seats)

                    Log.d(TAG, "Restored ${booking.seats} seats to trip ${booking.tripId}")
                }

                _bookingEvent.emit(BookingEvent.BookingRejected(bookingId, reason))

                // Reload incoming requests
                _currentUserId.value?.let { userId ->
                    loadIncomingBookingRequests(userId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error rejecting booking: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to reject booking"
            }
        }
    }

    /**
     * Debug function to check data integrity
     */
    fun debugBookingData(tripId: String? = null) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 === DEBUGGING BOOKING DATA ===")

                val currentUser = _currentUserId.value
                Log.d(TAG, "Current user ID: $currentUser")

                if (tripId != null) {
                    // Debug specific trip
                    val trip = tripRepository.getTripById(tripId).first()
                    Log.d(TAG, "Trip $tripId: driver=${trip?.driverId}, status=${trip?.status}")

                    tripRepository.getBookingsByTripId(tripId).take(1).collect { bookings ->
                        Log.d(TAG, "Trip $tripId has ${bookings.size} bookings:")
                        bookings.forEach { booking ->
                            Log.d(TAG, "  - Booking ${booking.bookingId}: passenger=${booking.passengerId}, status=${booking.status}")
                        }
                    }
                } else if (currentUser != null) {
                    // Debug current user's data
                    tripRepository.getTripsByDriverId(currentUser).take(1).collect { trips ->
                        Log.d(TAG, "Current user has ${trips.size} trips as driver")
                        trips.forEach { trip ->
                            Log.d(TAG, "  - Trip ${trip.tripId}: ${trip.status}")
                        }
                    }

                    tripRepository.getBookingsByPassengerId(currentUser).take(1).collect { bookings ->
                        Log.d(TAG, "Current user has ${bookings.size} bookings as passenger")
                        bookings.forEach { booking ->
                            Log.d(TAG, "  - Booking ${booking.bookingId}: trip=${booking.tripId}, status=${booking.status}")
                        }
                    }
                }

                Log.d(TAG, "🔍 === END DEBUG ===")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Debug error: ${e.message}", e)
            }
        }
    }

    suspend fun canUserBookTrip(tripId: String, userId: String): BookingEligibility {
        return try {
            val trip = tripRepository.getTripById(tripId).first()
            if (trip == null) {
                BookingEligibility.TripNotFound
            } else if (trip.driverId == userId) {
                BookingEligibility.CannotBookOwnTrip
            } else if (trip.status != "SCHEDULED") {
                BookingEligibility.TripNotAvailable
            } else if (trip.availableSeats <= 0) {
                BookingEligibility.NoSeatsAvailable
            } else {
                val existingBooking = tripRepository.getBookingByTripAndPassenger(tripId, userId)
                if (existingBooking != null) {
                    BookingEligibility.AlreadyBooked(existingBooking.status)
                } else {
                    BookingEligibility.Eligible
                }
            }
        } catch (e: Exception) {
            BookingEligibility.Error(e.message ?: "Unknown error")
        }
    }

    fun cancelBookingRequest(bookingId: String) {
        viewModelScope.launch {
            try {
                Log.d("BOOKING_REQUEST_VM", "Cancelling booking request: $bookingId")

                // Get the booking details
                val booking = tripRepository.getBookingById(bookingId).first()
                if (booking != null) {
                    // Update status to cancelled
                    tripRepository.updateBookingStatus(bookingId, "CANCELLED")

                    // If booking was approved, restore seats
                    if (booking.status == "APPROVED" || booking.status == "PENDING") {
                        tripRepository.increaseAvailableSeats(booking.tripId, booking.seats)
                    }
                }

                _bookingEvent.emit(BookingEvent.BookingCancelled(bookingId))

                // Reload user requests
                _currentUserId.value?.let { userId ->
                    loadUserBookingRequests(userId)
                }

            } catch (e: Exception) {
                Log.e("BOOKING_REQUEST_VM", "Error cancelling booking: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to cancel booking"
            }
        }
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        _currentUserId.value?.let { userId ->
            Log.d(TAG, "🔄 Refreshing all booking data for user: $userId")
            loadUserBookingRequests(userId)
            loadIncomingBookingRequests(userId)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Events emitted by the BookingRequestViewModel
 */
sealed class BookingEvent {
    data class BookingCreated(val booking: TripBooking) : BookingEvent()
    data class BookingApproved(val bookingId: String) : BookingEvent()
    data class BookingRejected(val bookingId: String, val reason: String?) : BookingEvent()
    data class BookingCancelled(val bookingId: String) : BookingEvent()
    data class Error(val message: String) : BookingEvent()
}

/**
 * Represents the eligibility of a user to book a trip
 */
sealed class BookingEligibility {
    object Eligible : BookingEligibility()
    object TripNotFound : BookingEligibility()
    object CannotBookOwnTrip : BookingEligibility()
    object TripNotAvailable : BookingEligibility()
    object NoSeatsAvailable : BookingEligibility()
    data class AlreadyBooked(val status: String) : BookingEligibility()
    data class Error(val message: String) : BookingEligibility()
}