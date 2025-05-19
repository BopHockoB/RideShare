package ua.nure.rideshare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

/**
 * Trip entity representing a ride offering or booking stored in the local database
 */
@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["userId"],
            childColumns = ["driver_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Car::class,
            parentColumns = ["carId"],
            childColumns = ["car_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Route::class,
            parentColumns = ["routeId"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("driver_id"), Index("car_id"), Index("route_id")]
)
data class Trip(
    @PrimaryKey
    val tripId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "driver_id")
    val driverId: String,

    @ColumnInfo(name = "car_id")
    val carId: String?,

    @ColumnInfo(name = "route_id")
    val routeId: String,

    @ColumnInfo(name = "departure_time")
    val departureTime: Long,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "available_seats")
    val availableSeats: Int,

    @ColumnInfo(name = "status")
    val status: String, // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurrence_pattern")
    val recurrencePattern: String? = null, // Daily, Weekly, etc.

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
