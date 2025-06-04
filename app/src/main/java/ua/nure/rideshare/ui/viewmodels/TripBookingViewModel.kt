package ua.nure.rideshare.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.TripBooking
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.repository.TripRepository
import ua.nure.rideshare.data.repository.UserRepository
import javax.inject.Inject

/**
 * ViewModel for managing trip bookings
 */
@HiltViewModel
class TripBookingViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Current user's bookings as passenger
    private val _userBookings = MutableStateFlow<List<TripBooking>>(emptyList())
    val userBookings: StateFlow<List<TripBooking>> = _userBookings

    // Bookings for a specific trip (for drivers to see who booked)
    private val _tripBookings = MutableStateFlow<List<TripBooking>>(emptyList())
    val tripBookings: StateFlow<List<TripBooking>> = _tripBookings

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Get bookings by passenger ID (Flow)
     */
    fun getBookingsByPassengerId(passengerId: String): Flow<List<TripBooking>> {
        return tripRepository.getBookingsByPassengerId(passengerId)
    }

    /**
     * Get bookings by trip ID (Flow)
     */
    fun getBookingsByTripId(tripId: String): Flow<List<TripBooking>> {
        return tripRepository.getBookingsByTripId(tripId)
    }

    /**
     * Get bookings by trip ID (Once)
     */
    suspend fun getBookingsByTripIdOnce(tripId: String): List<TripBooking> {
        return try {
            tripRepository.getBookingsByTripId(tripId).first()
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to load bookings"
            emptyList()
        }
    }

    /**
     * Get bookings by passenger ID and status
     */
    fun getBookingsByPassengerIdAndStatus(
        passengerId: String,
        status: String
    ): Flow<List<TripBooking>> {
        return tripRepository.getBookingsByPassengerIdAndStatus(passengerId, status)
    }

    /**
     * Load all bookings for a user as passenger
     */
    fun loadUserBookings(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tripRepository.getBookingsByPassengerId(userId).collect { bookings ->
                    _userBookings.value = bookings
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load bookings"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load bookings for a specific trip (for drivers)
     */
    fun loadTripBookings(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tripRepository.getBookingsByTripId(tripId).collect { bookings ->
                    _tripBookings.value = bookings
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load trip bookings"
                _isLoading.value = false
            }
        }
    }

    /**
     * Get a specific booking by ID
     */
    suspend fun getBookingById(bookingId: String): TripBooking? {
        return try {
            tripRepository.getBookingById(bookingId).first()
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to load booking"
            null
        }
    }

    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(bookingId: String) {
        try {
            tripRepository.updateBookingStatus(bookingId, "CANCELLED")
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to cancel booking"
            throw e
        }
    }

    /**
     * Update booking status (for drivers to approve/reject)
     */
    suspend fun updateBookingStatus(bookingId: String, status: String) {
        try {
            tripRepository.updateBookingStatus(bookingId, status)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to update booking status"
            throw e
        }
    }

    /**
     * Update payment status
     */
    suspend fun updatePaymentStatus(bookingId: String, paymentStatus: String) {
        try {
            tripRepository.updatePaymentStatus(bookingId, paymentStatus)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to update payment status"
            throw e
        }
    }

    /**
     * Add review and rating for a booking
     */
    suspend fun addReview(bookingId: String, rating: Float, review: String?) {
        try {
            tripRepository.updateBookingReview(bookingId, rating, review)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to add review"
            throw e
        }
    }

    /**
     * Get profile by ID (helper method)
     */
    suspend fun getProfileByIdOnce(userId: String): Profile? {
        return try {
            userRepository.getProfileById(userId).first()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}