package ua.nure.rideshare.data.repository

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.dao.CarDao
import ua.nure.rideshare.data.model.Car
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Car data
 */
@Singleton
class CarRepository @Inject constructor(
    private val carDao: CarDao
) {
    val allCars: Flow<List<Car>> = carDao.getAllCars()

    fun getCarById(carId: String): Flow<Car?> = carDao.getCarById(carId)

    fun getCarsByOwnerId(ownerId: String): Flow<List<Car>> = carDao.getCarsByOwnerId(ownerId)

    fun getActiveCarsByOwnerId(ownerId: String): Flow<List<Car>> = carDao.getActiveCarsByOwnerId(ownerId)

    @WorkerThread
    suspend fun insertCar(car: Car) {
        carDao.insertCar(car)
    }

    @WorkerThread
    suspend fun updateCar(car: Car) {
        carDao.updateCar(car)
    }

    @WorkerThread
    suspend fun deleteCar(car: Car) {
        carDao.deleteCar(car)
    }

    @WorkerThread
    suspend fun deleteCarById(carId: String) {
        carDao.deleteCarById(carId)
    }

    @WorkerThread
    suspend fun updateCarActiveStatus(carId: String, isActive: Boolean) {
        carDao.updateCarActiveStatus(carId, isActive)
    }

    @WorkerThread
    suspend fun updateCarPhoto(carId: String, photoUrl: String?) {
        carDao.updateCarPhoto(carId, photoUrl)
    }
}
