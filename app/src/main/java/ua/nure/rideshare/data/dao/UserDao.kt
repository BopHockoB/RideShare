package ua.nure.rideshare.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.model.User

/**
 * Data Access Object for User entity
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("UPDATE users SET last_login = :timestamp, updated_at = :timestamp WHERE userId = :userId")
    suspend fun updateUserLogin(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET refresh_token = :refreshToken, updated_at = :timestamp WHERE userId = :userId")
    suspend fun updateRefreshToken(userId: String, refreshToken: String?, timestamp: Long = System.currentTimeMillis())
}