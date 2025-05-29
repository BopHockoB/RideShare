package ua.nure.rideshare.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.data.repository.CarRepository
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for car management
 */
@HiltViewModel
class CarViewModel @Inject constructor(
    private val carRepository: CarRepository
) : ViewModel() {

    // User's cars
    private val _userCars = MutableStateFlow<List<Car>>(emptyList())
    val userCars: StateFlow<List<Car>> = _userCars

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Load all cars for a user
     */
    fun loadUserCars(userId: String) {
        Log.d("CAR_VIEWMODEL", "loadUserCars called with userId: $userId")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Use the flow from repository
                carRepository.getCarsByOwnerId(userId).collect { cars ->
                    Log.d("CAR_VIEWMODEL", "Loaded ${cars.size} cars for user $userId")
                    cars.forEachIndexed { index, car ->
                        Log.d("CAR_VIEWMODEL", "Car $index: ${car.carId} - ${car.make} ${car.model} - Active: ${car.isActive}")
                    }
                    _userCars.value = cars
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("CAR_VIEWMODEL", "Error loading cars: ${e.message}", e)
                _errorMessage.value = e.message ?: "Unknown error loading cars"
                _isLoading.value = false
            }
        }
    }

    /**
     * Get active cars for a user
     */
    fun loadActiveUserCars(userId: String) {
        Log.d("CAR_VIEWMODEL", "loadActiveUserCars called with userId: $userId")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Use the flow from repository
                carRepository.getActiveCarsByOwnerId(userId).collect { cars ->
                    Log.d("CAR_VIEWMODEL", "Loaded ${cars.size} active cars for user $userId")
                    cars.forEachIndexed { index, car ->
                        Log.d("CAR_VIEWMODEL", "Active Car $index: ${car.carId} - ${car.make} ${car.model}")
                    }
                    _userCars.value = cars
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("CAR_VIEWMODEL", "Error loading active cars: ${e.message}", e)
                _errorMessage.value = e.message ?: "Unknown error loading active cars"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load a specific car by ID (useful for debugging)
     */
    fun loadCarById(carId: String) {
        Log.d("CAR_VIEWMODEL", "loadCarById called with carId: $carId")
        viewModelScope.launch {
            try {
                carRepository.getCarById(carId).take(1).collect { car ->
                    if (car != null) {
                        Log.d("CAR_VIEWMODEL", "Found car: ${car.make} ${car.model} - Owner: ${car.ownerId} - Active: ${car.isActive}")
                    } else {
                        Log.w("CAR_VIEWMODEL", "Car not found for ID: $carId")
                    }
                }
            } catch (e: Exception) {
                Log.e("CAR_VIEWMODEL", "Error loading car by ID: ${e.message}", e)
            }
        }
    }

    /**
     * Get a specific car and add it to the current list (for when we need a specific car)
     */
    fun addSpecificCarToList(carId: String) {
        Log.d("CAR_VIEWMODEL", "addSpecificCarToList called with carId: $carId")
        viewModelScope.launch {
            try {
                carRepository.getCarById(carId).take(1).collect { car ->
                    if (car != null) {
                        Log.d("CAR_VIEWMODEL", "Adding specific car to list: ${car.make} ${car.model}")
                        val currentCars = _userCars.value.toMutableList()
                        // Only add if not already in the list
                        if (currentCars.none { it.carId == carId }) {
                            currentCars.add(car)
                            _userCars.value = currentCars
                            Log.d("CAR_VIEWMODEL", "Car added to list. Total cars: ${currentCars.size}")
                        } else {
                            Log.d("CAR_VIEWMODEL", "Car already in list")
                        }
                    } else {
                        Log.w("CAR_VIEWMODEL", "Cannot add car - not found for ID: $carId")
                    }
                }
            } catch (e: Exception) {
                Log.e("CAR_VIEWMODEL", "Error adding specific car: ${e.message}", e)
            }
        }
    }

    /**
     * Create a new car
     */
    suspend fun createCar(
        ownerId: String,
        make: String,
        model: String,
        year: Int,
        color: String,
        licensePlate: String,
        photoUrl: String?,
        seats: Int,
        amenities: String?,
        isActive: Boolean = true
    ): String {
        val carId = UUID.randomUUID().toString()
        val car = Car(
            carId = carId,
            ownerId = ownerId,
            make = make,
            model = model,
            year = year,
            color = color,
            licensePlate = licensePlate,
            photoUrl = photoUrl,
            seats = seats,
            amenities = amenities,
            isActive = isActive,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        Log.d("CAR_VIEWMODEL", "Creating car: ${make} ${model} for owner: ${ownerId}")
        carRepository.insertCar(car)
        return carId
    }


    suspend fun getCarById(carId: String): Car? {
        Log.d("CAR_VIEWMODEL", "Updating car: ${carId}")
         return carRepository.getCarById(carId).first()
    }
    /**
     * Update car details
     */
    suspend fun updateCar(car: Car) {
        Log.d("CAR_VIEWMODEL", "Updating car: ${car.carId}")
        carRepository.updateCar(car)
    }

    /**
     * Delete a car
     */
    suspend fun deleteCar(car: Car) {
        Log.d("CAR_VIEWMODEL", "Deleting car: ${car.carId}")
        carRepository.deleteCar(car)
    }

    /**
     * Update a car's active status
     */
    suspend fun updateCarActiveStatus(carId: String, isActive: Boolean) {
        Log.d("CAR_VIEWMODEL", "Updating car active status: $carId -> $isActive")
        carRepository.updateCarActiveStatus(carId, isActive)
    }

    /**
     * Update a car's photo
     */
    suspend fun updateCarPhoto(carId: String, photoUrl: String?) {
        Log.d("CAR_VIEWMODEL", "Updating car photo: $carId")
        carRepository.updateCarPhoto(carId, photoUrl)
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}