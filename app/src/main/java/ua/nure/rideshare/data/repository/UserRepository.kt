package ua.nure.rideshare.data.repository

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.dao.UserDao
import ua.nure.rideshare.data.dao.ProfileDao
import ua.nure.rideshare.data.model.User
import ua.nure.rideshare.data.model.Profile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for User and Profile data
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val profileDao: ProfileDao
) {
    // User related methods
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    fun getUserById(userId: String): Flow<User?> = userDao.getUserById(userId)

    @WorkerThread
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)

    @WorkerThread
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    @WorkerThread
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    @WorkerThread
    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    @WorkerThread
    suspend fun updateUserLogin(userId: String) {
        userDao.updateUserLogin(userId)
    }

    @WorkerThread
    suspend fun updateRefreshToken(userId: String, refreshToken: String?) {
        userDao.updateRefreshToken(userId, refreshToken)
    }

    // Profile related methods
    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfiles()

    fun getProfileById(userId: String): Flow<Profile?> = profileDao.getProfileById(userId)

    @WorkerThread
    suspend fun getProfileByEmail(email: String): Profile? = profileDao.getProfileByEmail(email)

    @WorkerThread
    suspend fun insertProfile(profile: Profile) {
        profileDao.insertProfile(profile)
    }

    @WorkerThread
    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile)
    }

    @WorkerThread
    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }

    @WorkerThread
    suspend fun updateProfileRating(userId: String, rating: Float) {
        profileDao.updateProfileRating(userId, rating)
    }

    @WorkerThread
    suspend fun incrementTripsCount(userId: String) {
        profileDao.incrementTripsCount(userId)
    }

    fun searchProfiles(query: String): Flow<List<Profile>> {
        return profileDao.searchProfiles("%$query%")
    }
}
