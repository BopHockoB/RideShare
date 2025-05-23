package ua.nure.rideshare.ui.viewmodels


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
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Use the flow from repository
                carRepository.getCarsByOwnerId(userId).collect { cars ->
                    _userCars.value = cars
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading cars"
                _isLoading.value = false
            }
        }
    }

    /**
     * Get active cars for a user
     */
    fun loadActiveUserCars(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Use the flow from repository
                carRepository.getActiveCarsByOwnerId(userId).collect { cars ->
                    _userCars.value = cars
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading active cars"
                _isLoading.value = false
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

        carRepository.insertCar(car)
        return carId
    }

    /**
     * Update car details
     */
    suspend fun updateCar(car: Car) {
        carRepository.updateCar(car)
    }

    /**
     * Delete a car
     */
    suspend fun deleteCar(car: Car) {
        carRepository.deleteCar(car)
    }

    /**
     * Update a car's active status
     */
    suspend fun updateCarActiveStatus(carId: String, isActive: Boolean) {
        carRepository.updateCarActiveStatus(carId, isActive)
    }

    /**
     * Update a car's photo
     */
    suspend fun updateCarPhoto(carId: String, photoUrl: String?) {
        carRepository.updateCarPhoto(carId, photoUrl)
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}