package ua.nure.rideshare.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.model.Profile

/**
 * Data Access Object for Profile entity
 */
@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun getProfileById(userId: String): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE email = :email")
    suspend fun getProfileByEmail(email: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("DELETE FROM profiles WHERE userId = :userId")
    suspend fun deleteProfileById(userId: String)

    @Query("UPDATE profiles SET rating = :rating, updated_at = :timestamp WHERE userId = :userId")
    suspend fun updateProfileRating(userId: String, rating: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE profiles SET trips_count = trips_count + 1, updated_at = :timestamp WHERE userId = :userId")
    suspend fun incrementTripsCount(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM profiles WHERE first_name LIKE :searchQuery OR last_name LIKE :searchQuery")
    fun searchProfiles(searchQuery: String): Flow<List<Profile>>
}