package ua.nure.rideshare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

/**
 * Profile entity representing user information stored in the local database
 */
@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Profile(
    @PrimaryKey
    val userId: String, // Primary key is also a foreign key to User.userId

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "profile_photo_url")
    val profilePhotoUrl: String? = null,

    @ColumnInfo(name = "gender")
    val gender: String, // MALE or FEMALE

    @ColumnInfo(name = "age")
    val age: Int,

    @ColumnInfo(name = "bio")
    val bio: String? = null,

    @ColumnInfo(name = "driving_experience")
    val drivingExperience: Int? = null,

    @ColumnInfo(name = "conversation_style")
    val conversationStyle: String? = null,

    @ColumnInfo(name = "rating")
    val rating: Float = 0.0f,

    @ColumnInfo(name = "trips_count")
    val tripsCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
