package ua.nure.rideshare.data.repository

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.dao.TripDao
import ua.nure.rideshare.data.dao.TripBookingDao
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.TripBooking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Trip and TripBooking data
 */
@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val tripBookingDao: TripBookingDao
) {
    // Trip related methods
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()
    val upcomingTrips: Flow<List<Trip>> = tripDao.getUpcomingTrips()

    fun getTripById(tripId: String): Flow<Trip?> = tripDao.getTripById(tripId)

    fun getTripsByDriverId(driverId: String): Flow<List<Trip>> = tripDao.getTripsByDriverId(driverId)

    fun getTripsByDriverIdAndStatus(driverId: String, status: String): Flow<List<Trip>> =
        tripDao.getTripsByDriverIdAndStatus(driverId, status)

    fun getTripsByRouteId(routeId: String): Flow<List<Trip>> = tripDao.getTripsByRouteId(routeId)

    fun getAvailableTrips(requiredSeats: Int = 1): Flow<List<Trip>> =
        tripDao.getAvailableTrips(requiredSeats)

    @WorkerThread
    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip)
    }

    @WorkerThread
    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
    }

    @WorkerThread
    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
    }

    @WorkerThread
    suspend fun deleteTripById(tripId: String) {
        tripDao.deleteTripById(tripId)
    }

    @WorkerThread
    suspend fun updateTripStatus(tripId: String, status: String) {
        tripDao.updateTripStatus(tripId, status)
    }

    @WorkerThread
    suspend fun decreaseAvailableSeats(tripId: String, bookedSeats: Int) {
        tripDao.decreaseAvailableSeats(tripId, bookedSeats)
    }

    @WorkerThread
    suspend fun increaseAvailableSeats(tripId: String, cancelledSeats: Int) {
        tripDao.increaseAvailableSeats(tripId, cancelledSeats)
    }

    // TripBooking related methods
    val allBookings: Flow<List<TripBooking>> = tripBookingDao.getAllBookings()

    fun getBookingById(bookingId: String): Flow<TripBooking?> =
        tripBookingDao.getBookingById(bookingId)

    fun getBookingsByTripId(tripId: String): Flow<List<TripBooking>> =
        tripBookingDao.getBookingsByTripId(tripId)

    fun getBookingsByPassengerId(passengerId: String): Flow<List<TripBooking>> =
        tripBookingDao.getBookingsByPassengerId(passengerId)

    fun getBookingsByPassengerIdAndStatus(passengerId: String, status: String): Flow<List<TripBooking>> =
        tripBookingDao.getBookingsByPassengerIdAndStatus(passengerId, status)

    @WorkerThread
    suspend fun getBookingByTripAndPassenger(tripId: String, passengerId: String): TripBooking? =
        tripBookingDao.getBookingByTripAndPassenger(tripId, passengerId)

    @WorkerThread
    suspend fun insertBooking(booking: TripBooking) {
        tripBookingDao.insertBooking(booking)
        // When a booking is made, decrease the available seats on the trip
        decreaseAvailableSeats(booking.tripId, booking.seats)
    }

    @WorkerThread
    suspend fun updateBooking(booking: TripBooking) {
        tripBookingDao.updateBooking(booking)
    }

    @WorkerThread
    suspend fun deleteBooking(booking: TripBooking) {
        tripBookingDao.deleteBooking(booking)
        // When a booking is deleted, increase the available seats on the trip
        if (booking.status != "COMPLETED") {
            increaseAvailableSeats(booking.tripId, booking.seats)
        }
    }

    @WorkerThread
    suspend fun deleteBookingById(bookingId: String, tripId: String, seats: Int) {
        tripBookingDao.deleteBookingById(bookingId)
        // When a booking is deleted, increase the available seats on the trip
        increaseAvailableSeats(tripId, seats)
    }

    @WorkerThread
    suspend fun updateBookingStatus(bookingId: String, status: String) {
        tripBookingDao.updateBookingStatus(bookingId, status)
    }

    @WorkerThread
    suspend fun updatePaymentStatus(bookingId: String, paymentStatus: String) {
        tripBookingDao.updatePaymentStatus(bookingId, paymentStatus)
    }

    @WorkerThread
    suspend fun updateBookingReview(bookingId: String, rating: Float, review: String?) {
        tripBookingDao.updateBookingReview(bookingId, rating, review)
    }

    /**
     * Book a trip
     *
     * @param booking The booking to create
     * @return The created booking
     */
    @WorkerThread
    suspend fun bookTrip(booking: TripBooking): TripBooking {
        // Check if there's an existing booking
        val existingBooking = getBookingByTripAndPassenger(booking.tripId, booking.passengerId)
        if (existingBooking != null) {
            // Update the existing booking
            val updatedBooking = existingBooking.copy(
                seats = booking.seats,
                pickupLocation = booking.pickupLocation,
                pickupLatitude = booking.pickupLatitude,
                pickupLongitude = booking.pickupLongitude,
                dropoffLocation = booking.dropoffLocation,
                dropoffLatitude = booking.dropoffLatitude,
                dropoffLongitude = booking.dropoffLongitude,
                status = "PENDING",
                updatedAt = System.currentTimeMillis()
            )
            updateBooking(updatedBooking)
            return updatedBooking
        } else {
            // Create a new booking
            insertBooking(booking)
            return booking
        }
    }
}
