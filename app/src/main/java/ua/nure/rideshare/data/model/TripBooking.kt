package ua.nure.rideshare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

/**
 * TripBooking entity representing a passenger's booking of a trip
 */
@Entity(
    tableName = "trip_bookings",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["tripId"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["userId"],
            childColumns = ["passenger_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trip_id"), Index("passenger_id")]
)
data class TripBooking(
    @PrimaryKey
    val bookingId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "trip_id")
    val tripId: String,

    @ColumnInfo(name = "passenger_id")
    val passengerId: String,

    @ColumnInfo(name = "seats")
    val seats: Int = 1,

    @ColumnInfo(name = "pickup_location")
    val pickupLocation: String? = null,

    @ColumnInfo(name = "pickup_latitude")
    val pickupLatitude: Double? = null,

    @ColumnInfo(name = "pickup_longitude")
    val pickupLongitude: Double? = null,

    @ColumnInfo(name = "dropoff_location")
    val dropoffLocation: String? = null,

    @ColumnInfo(name = "dropoff_latitude")
    val dropoffLatitude: Double? = null,

    @ColumnInfo(name = "dropoff_longitude")
    val dropoffLongitude: Double? = null,

    @ColumnInfo(name = "status")
    val status: String, // PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED

    @ColumnInfo(name = "payment_status")
    val paymentStatus: String = "PENDING", // PENDING, PAID, REFUNDED

    @ColumnInfo(name = "rating")
    val rating: Float? = null,

    @ColumnInfo(name = "review")
    val review: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
