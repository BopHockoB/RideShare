package ua.nure.rideshare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

/**
 * User entity representing authentication and account information stored in the local database
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "is_email_verified")
    val isEmailVerified: Boolean = false,

    @ColumnInfo(name = "auth_provider")
    val authProvider: String = "EMAIL", // EMAIL, GOOGLE, FACEBOOK

    @ColumnInfo(name = "provider_user_id")
    val providerUserId: String? = null, // For OAuth providers

    @ColumnInfo(name = "refresh_token")
    val refreshToken: String? = null,

    @ColumnInfo(name = "last_login")
    val lastLogin: Long? = null,

    @ColumnInfo(name = "account_status")
    val accountStatus: String = "ACTIVE", // ACTIVE, SUSPENDED, INACTIVE, BANNED

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
