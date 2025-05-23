package ua.nure.rideshare.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class for simplified autocomplete prediction
 */
data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

/**
 * ViewModel for Google Places API integration
 */
@HiltViewModel
class PlacesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Places client for making API requests
    private val placesClient: PlacesClient by lazy {
        Places.createClient(context)
    }

    // Session token for autocomplete requests
    private var sessionToken = AutocompleteSessionToken.newInstance()

    // Autocomplete predictions
    private val _predictions = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val predictions: StateFlow<List<PlacePrediction>> = _predictions

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Get autocomplete predictions for a query
     */
    fun getAutocompletePredictions(query: String) {
        if (query.isBlank()) {
            _predictions.value = emptyList()
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        try {
            // Create request
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build()

            // Execute request and handle response
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    // Map response to simplified predictions
                    val simplifiedPredictions = response.autocompletePredictions.map { prediction ->
                        PlacePrediction(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString()
                        )
                    }

                    _predictions.value = simplifiedPredictions
                    _isLoading.value = false
                }
                .addOnFailureListener { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                    _predictions.value = emptyList()
                }
        } catch (e: Exception) {
            _errorMessage.value = e.message
            _isLoading.value = false
            _predictions.value = emptyList()
        }
    }

    /**
     * Get place details for a place ID
     */
    fun getPlaceDetails(placeId: String, callback: (Place?) -> Unit) {
        try {
            // Specify the fields to return
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )

            // Create request
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)

            // Execute request and handle response
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    callback(response.place)
                    // Create a new session token after a place is fetched
                    sessionToken = AutocompleteSessionToken.newInstance()
                }
                .addOnFailureListener { exception ->
                    _errorMessage.value = exception.message
                    callback(null)
                }
        } catch (e: Exception) {
            _errorMessage.value = e.message
            callback(null)
        }
    }

    /**
     * Clear predictions
     */
    fun clearPredictions() {
        _predictions.value = emptyList()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
