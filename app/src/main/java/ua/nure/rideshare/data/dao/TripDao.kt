package ua.nure.rideshare.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.model.Trip

/**
 * Data Access Object for Trip entity
 */
@Dao
interface TripDao {
    @Query("SELECT * FROM trips")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE tripId = :tripId")
    fun getTripById(tripId: String): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE driver_id = :driverId")
    fun getTripsByDriverId(driverId: String): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE driver_id = :driverId AND status = :status")
    fun getTripsByDriverIdAndStatus(driverId: String, status: String): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE route_id = :routeId")
    fun getTripsByRouteId(routeId: String): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE status = 'SCHEDULED' AND departure_time >= :currentTime")
    fun getUpcomingTrips(currentTime: Long = System.currentTimeMillis()): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE status = 'SCHEDULED' AND departure_time >= :currentTime AND available_seats >= :requiredSeats")
    fun getAvailableTrips(requiredSeats: Int = 1, currentTime: Long = System.currentTimeMillis()): Flow<List<Trip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE tripId = :tripId")
    suspend fun deleteTripById(tripId: String)

    @Query("UPDATE trips SET status = :status, updated_at = :timestamp WHERE tripId = :tripId")
    suspend fun updateTripStatus(tripId: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE trips SET available_seats = available_seats - :bookedSeats, updated_at = :timestamp WHERE tripId = :tripId")
    suspend fun decreaseAvailableSeats(tripId: String, bookedSeats: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE trips SET available_seats = available_seats + :cancelledSeats, updated_at = :timestamp WHERE tripId = :tripId")
    suspend fun increaseAvailableSeats(tripId: String, cancelledSeats: Int, timestamp: Long = System.currentTimeMillis())
}
