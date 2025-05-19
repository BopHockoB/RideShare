package ua.nure.rideshare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

/**
 * Route entity representing a path between locations stored in the local database
 */
@Entity(
    tableName = "routes",
    indices = [Index("start_location"), Index("end_location")]
)
data class Route(
    @PrimaryKey
    val routeId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "start_location")
    val startLocation: String, // Name of start location (e.g., "Current Location")

    @ColumnInfo(name = "start_address")
    val startAddress: String, // Full address of start location

    @ColumnInfo(name = "start_latitude")
    val startLatitude: Double,

    @ColumnInfo(name = "start_longitude")
    val startLongitude: Double,

    @ColumnInfo(name = "end_location")
    val endLocation: String, // Name of end location (e.g., "Office")

    @ColumnInfo(name = "end_address")
    val endAddress: String, // Full address of end location

    @ColumnInfo(name = "end_latitude")
    val endLatitude: Double,

    @ColumnInfo(name = "end_longitude")
    val endLongitude: Double,

    @ColumnInfo(name = "distance")
    val distance: Float, // Distance in kilometers

    @ColumnInfo(name = "duration")
    val duration: Int, // Duration in minutes

    @ColumnInfo(name = "polyline")
    val polyline: String? = null, // Encoded polyline for drawing the route

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
