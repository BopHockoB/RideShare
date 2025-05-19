package ua.nure.rideshare.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.model.TripBooking

/**
 * Data Access Object for TripBooking entity
 */
@Dao
interface TripBookingDao {
    @Query("SELECT * FROM trip_bookings")
    fun getAllBookings(): Flow<List<TripBooking>>

    @Query("SELECT * FROM trip_bookings WHERE bookingId = :bookingId")
    fun getBookingById(bookingId: String): Flow<TripBooking?>

    @Query("SELECT * FROM trip_bookings WHERE trip_id = :tripId")
    fun getBookingsByTripId(tripId: String): Flow<List<TripBooking>>

    @Query("SELECT * FROM trip_bookings WHERE passenger_id = :passengerId")
    fun getBookingsByPassengerId(passengerId: String): Flow<List<TripBooking>>

    @Query("SELECT * FROM trip_bookings WHERE passenger_id = :passengerId AND status = :status")
    fun getBookingsByPassengerIdAndStatus(passengerId: String, status: String): Flow<List<TripBooking>>

    @Query("SELECT * FROM trip_bookings WHERE trip_id = :tripId AND passenger_id = :passengerId")
    suspend fun getBookingByTripAndPassenger(tripId: String, passengerId: String): TripBooking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: TripBooking)

    @Update
    suspend fun updateBooking(booking: TripBooking)

    @Delete
    suspend fun deleteBooking(booking: TripBooking)

    @Query("DELETE FROM trip_bookings WHERE bookingId = :bookingId")
    suspend fun deleteBookingById(bookingId: String)

    @Query("UPDATE trip_bookings SET status = :status, updated_at = :timestamp WHERE bookingId = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE trip_bookings SET payment_status = :paymentStatus, updated_at = :timestamp WHERE bookingId = :bookingId")
    suspend fun updatePaymentStatus(bookingId: String, paymentStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE trip_bookings SET rating = :rating, review = :review, updated_at = :timestamp WHERE bookingId = :bookingId")
    suspend fun updateBookingReview(bookingId: String, rating: Float, review: String?, timestamp: Long = System.currentTimeMillis())
}
