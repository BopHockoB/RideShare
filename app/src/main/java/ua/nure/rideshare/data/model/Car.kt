package ua.nure.rideshare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

/**
 * Car entity representing vehicle information stored in the local database
 */
@Entity(
    tableName = "cars",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["userId"],
            childColumns = ["owner_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("owner_id")]
)
data class Car(
    @PrimaryKey
    val carId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    @ColumnInfo(name = "make")
    val make: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "license_plate")
    val licensePlate: String,

    @ColumnInfo(name = "photo_url")
    val photoUrl: String? = null,

    @ColumnInfo(name = "seats")
    val seats: Int,

    @ColumnInfo(name = "amenities")
    val amenities: String? = null, // Stored as comma-separated values

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
