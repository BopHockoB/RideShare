package ua.nure.rideshare.ui.screens.location

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import ua.nure.rideshare.ui.viewmodels.PlacesViewModel

/**
 * Data class representing a location with address and coordinates
 */
data class LocationData(
    val name: String,
    val address: String,
    val latLng: LatLng
)

/**
 * Screen for selecting a location with address autocomplete
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, FlowPreview::class)
@Composable
fun LocationSelectionScreen(
    initialQuery: String = "",
    onLocationSelected: (LocationData) -> Unit,
    onDismiss: () -> Unit,
    locationViewModel: LocationViewModel,
    placesViewModel: PlacesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf(initialQuery) }
    val predictions by placesViewModel.predictions.collectAsState()
    val isLoading by placesViewModel.isLoading.collectAsState()
    val userLocation by locationViewModel.userLocation.collectAsState()

    // Current location data
    val currentLocationData = remember(userLocation) {
        userLocation?.let {
            LocationData(
                name = "Current Location",
                address = "Use your current location",
                latLng = it
            )
        }
    }

    // Recent locations - in a real app, these would come from a database
    val recentLocations = remember {
        listOf(
            LocationData(
                name = "Home",
                address = "123 Home Street, Anytown, USA",
                latLng = LatLng(40.7128, -74.0060)
            ),
            LocationData(
                name = "Office",
                address = "456 Office Avenue, Worktown, USA",
                latLng = LatLng(40.7168, -74.0030)
            )
        )
    }

    // Debounce search query to reduce API calls
    val debouncedQuery = remember {
        MutableStateFlow(searchQuery)
    }

    LaunchedEffect(debouncedQuery) {
        debouncedQuery
            .debounce(300) // 300ms debounce
            .collect { query ->
                if (query.isNotBlank() && query.length >= 3) {
                    placesViewModel.getAutocompletePredictions(query)
                }
            }
    }

    // Update debounced query when searchQuery changes
    LaunchedEffect(searchQuery) {
        debouncedQuery.value = searchQuery
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search for a location") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF00A16B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (searchQuery.isNotBlank()) {
                            placesViewModel.getAutocompletePredictions(searchQuery)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = Color(0xFF00A16B),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00A16B))
                }
            }

            // Current location option
            if (currentLocationData != null) {
                LocationOption(
                    name = "Current Location",
                    address = "Use your current location",
                    icon = Icons.Default.MyLocation,
                    iconTint = Color(0xFF00A16B),
                    onClick = {
                        onLocationSelected(currentLocationData)
                    }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }

            // Recent locations section if search is empty
            if (searchQuery.isBlank() && recentLocations.isNotEmpty()) {
                Text(
                    text = "Recent locations",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // List of recent locations
                recentLocations.forEach { location ->
                    LocationOption(
                        name = location.name,
                        address = location.address,
                        icon = Icons.Default.History,
                        iconTint = Color.Gray,
                        onClick = {
                            onLocationSelected(location)
                        }
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }

            // Search results
            if (searchQuery.isNotBlank() && predictions.isNotEmpty()) {
                Text(
                    text = "Search results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // List of predictions
                LazyColumn {
                    items(predictions) { prediction ->
                        LocationOption(
                            name = prediction.primaryText,
                            address = prediction.secondaryText,
                            icon = Icons.Default.LocationOn,
                            iconTint = Color.Gray,
                            onClick = {
                                // Get place details including lat/lng
                                placesViewModel.getPlaceDetails(prediction.placeId) { place ->
                                    place?.let {
                                        val locationData = LocationData(
                                            name = prediction.primaryText,
                                            address = prediction.secondaryText,
                                            latLng = it.latLng!!
                                        )
                                        onLocationSelected(locationData)
                                    }
                                }
                            }
                        )
                    }
                }
            } else if (searchQuery.isNotBlank() && !isLoading && predictions.isEmpty()) {
                // No results found
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "No locations found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationOption(
    name: String,
    address: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.padding(end = 16.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}