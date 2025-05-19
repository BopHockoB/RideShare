package ua.nure.rideshare.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.model.Car

/**
 * Data Access Object for Car entity
 */
@Dao
interface CarDao {
    @Query("SELECT * FROM cars")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE carId = :carId")
    fun getCarById(carId: String): Flow<Car?>

    @Query("SELECT * FROM cars WHERE owner_id = :ownerId")
    fun getCarsByOwnerId(ownerId: String): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE owner_id = :ownerId AND is_active = 1")
    fun getActiveCarsByOwnerId(ownerId: String): Flow<List<Car>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car)

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("DELETE FROM cars WHERE carId = :carId")
    suspend fun deleteCarById(carId: String)

    @Query("UPDATE cars SET is_active = :isActive, updated_at = :timestamp WHERE carId = :carId")
    suspend fun updateCarActiveStatus(carId: String, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cars SET photo_url = :photoUrl, updated_at = :timestamp WHERE carId = :carId")
    suspend fun updateCarPhoto(carId: String, photoUrl: String?, timestamp: Long = System.currentTimeMillis())
}
