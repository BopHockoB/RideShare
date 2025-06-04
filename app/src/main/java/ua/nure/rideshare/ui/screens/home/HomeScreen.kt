package ua.nure.rideshare.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.nure.rideshare.R
import ua.nure.rideshare.ui.screens.location.LocationData
import ua.nure.rideshare.ui.screens.location.LocationSelectionScreen
import ua.nure.rideshare.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun HomeScreen(
    locationViewModel: LocationViewModel,
    homeViewModel: HomeViewModel = hiltViewModel(),
    placesViewModel: PlacesViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    onNavigateToSearch: (String, String) -> Unit,
    onNavigateToRideDetails: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToBookingManagement: () -> Unit,
    onNavigateToYourRides: () -> Unit,
    onNavigateToCreateRide: () -> Unit
) {
    // UI state
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Rider") }
    var showTripsList by remember { mutableStateOf(false) }
    var showLocationSelector by remember { mutableStateOf(false) }
    var showAutocompleteDropdown by remember { mutableStateOf(false) }

    // Location state for trip search
    var tripSearchFromLocation by remember { mutableStateOf("Current Location") }
    var tripSearchFromAddress by remember { mutableStateOf("") }
    var tripSearchFromLat by remember { mutableStateOf(0.0) }
    var tripSearchFromLng by remember { mutableStateOf(0.0) }

    var tripSearchToLocation by remember { mutableStateOf("") }
    var tripSearchToAddress by remember { mutableStateOf("") }
    var tripSearchToLat by remember { mutableStateOf(0.0) }
    var tripSearchToLng by remember { mutableStateOf(0.0) }

//    var pendingRequestsCount

    var isSelectingFromLocation by remember { mutableStateOf(true) }

    // Get context for Toast
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect user location from LocationViewModel
    val userLocation by locationViewModel.userLocation.collectAsState(initial = null)
    val isLocationLoading by locationViewModel.isLoading.collectAsState()

    // Collect trips from HomeViewModel
    val availableTrips by homeViewModel.availableTrips.collectAsState()
    val nearbyLocationsState by homeViewModel.nearbyLocationsState.collectAsState()

    // Collect search results and autocomplete predictions
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isLoading.collectAsState()
    val predictions by placesViewModel.predictions.collectAsState()

    // Update trip search location when user location changes
    LaunchedEffect(userLocation) {
        if (tripSearchFromLocation == "Current Location" && userLocation != null) {
            tripSearchFromLat = userLocation!!.latitude
            tripSearchFromLng = userLocation!!.longitude
            tripSearchFromAddress = "Your current location"
        }
    }

    // Debounce search query for autocomplete
    val debouncedQuery = remember { MutableStateFlow(searchQuery) }

    LaunchedEffect(debouncedQuery) {
        debouncedQuery
            .debounce(300) // 300ms debounce
            .filter { it.isNotBlank() && it.length >= 3 }
            .collect { query ->
                placesViewModel.getAutocompletePredictions(query)
                showAutocompleteDropdown = true
            }
    }

    LaunchedEffect(searchQuery) {
        debouncedQuery.value = searchQuery
        if (searchQuery.isBlank()) {
            showAutocompleteDropdown = false
            placesViewModel.clearPredictions()
        }
    }

    // Effect to collect nearby trips when location changes
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            // Get nearby trips when user location is available
            homeViewModel.searchTrips(
                startLatitude = location.latitude,
                startLongitude = location.longitude,
                endLatitude = location.latitude + 0.1, // Just a placeholder, would be actual destination
                endLongitude = location.longitude + 0.1
            )
        }
    }

    // Effect to show toast for errors
    LaunchedEffect(nearbyLocationsState) {
        if (nearbyLocationsState is NearbyLocationsState.Error) {
            Toast.makeText(
                context,
                (nearbyLocationsState as NearbyLocationsState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Map state
    val defaultLocation = LatLng(50.4501, 30.5234) // Kyiv, Ukraine coordinates
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: defaultLocation, 15f)
    }

    // Update camera when user location changes
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            if (!cameraPositionState.isMoving) {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        }
    }

    // Start location updates when screen is shown
    LaunchedEffect(Unit) {
        locationViewModel.startLocationUpdates()
    }

    // Stop location updates when screen is left
    DisposableEffect(Unit) {
        onDispose {
            locationViewModel.stopLocationUpdates()
        }
    }

    // Load map style
    val mapStyleOptions = remember {
        try {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
        } catch (e: Exception) {
            null // Return null if style fails to load
        }
    }

    // Map properties
    val mapProperties = remember {
        mutableStateOf(
            MapProperties(
                isBuildingEnabled = true,
                isIndoorEnabled = true,
                isTrafficEnabled = false,
                mapStyleOptions = mapStyleOptions,
                mapType = MapType.NORMAL,
                maxZoomPreference = 20.0f,
                minZoomPreference = 3.0f
            )
        )
    }

    // Map UI settings
    val mapUiSettings = remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = true,
                zoomGesturesEnabled = true
            )
        )
    }

    // Map ready state
    var isMapReady by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties.value,
            uiSettings = mapUiSettings.value,
            onMapLoaded = {
                isMapReady = true
            }
        ) {
            // User location marker
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "You are here",
                    snippet = "Current location"
                )
            }

            // Trip markers
            when (nearbyLocationsState) {
                is NearbyLocationsState.Success -> {
                    val trips = (nearbyLocationsState as NearbyLocationsState.Success).trips
                    trips.forEach { trip ->
                        // We would need to join with routes to get locations
                        // This would be implemented in a real app
                    }
                }
                else -> {
                    // No markers to add
                }
            }
        }

        // Map loading indicator
        if (!isMapReady || isLocationLoading || nearbyLocationsState is NearbyLocationsState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00A16B)
                )
            }
        }

        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Menu button
            IconButton(
                onClick = { /* Open drawer */ },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00A16B))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            // Notification button
            IconButton(
                onClick = { /* Open notifications */ },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black
                )
            }
        }

        // Bottom section with search bar and tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Search box and mode selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Search bar with autocomplete
                    Box {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Where would you go?") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        showAutocompleteDropdown = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.Gray
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorites",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00A16B),
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = Color(0xFF00A16B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        // Autocomplete dropdown
                        if (showAutocompleteDropdown && predictions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 56.dp)
                                    .heightIn(max = 200.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                LazyColumn {
                                    items(predictions) { prediction ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Get place details and navigate to search
                                                    placesViewModel.getPlaceDetails(prediction.placeId) { place ->
                                                        place?.let {
                                                            val origin = userLocation?.let { loc ->
                                                                "${loc.latitude},${loc.longitude}"
                                                            } ?: ""
                                                            val destination = "${it.latLng?.latitude},${it.latLng?.longitude}"
                                                            onNavigateToSearch(origin, destination)
                                                        }
                                                    }
                                                    searchQuery = ""
                                                    showAutocompleteDropdown = false
                                                }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = prediction.primaryText,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = prediction.secondaryText,
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        if (predictions.last() != prediction) {
                                            Divider()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Rider / Driver toggle
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Rider button
                        Button(
                            onClick = {
                                selectedMode = "Rider"
                                // Refresh nearby rides
                                userLocation?.let { location ->
                                    homeViewModel.searchTrips(
                                        startLatitude = location.latitude,
                                        startLongitude = location.longitude,
                                        endLatitude = location.latitude + 0.1,
                                        endLongitude = location.longitude + 0.1
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMode == "Rider") Color(0xFF00A16B) else Color.White,
                                contentColor = if (selectedMode == "Rider") Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Rider")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Driver button
                        Button(
                            onClick = {
                                selectedMode = "Driver"
                                // If driver mode is selected, navigate to create ride screen
                                onNavigateToCreateRide()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMode == "Driver") Color(0xFF00A16B) else Color.White,
                                contentColor = if (selectedMode == "Driver") Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Driver")
                        }
                    }
                }
            }

            // Bottom navigation bar
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF00A16B)
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color(0xFF00A16B)
                        )
                    },
                    label = {
                        Text(
                            "Home",
                            color = Color(0xFF00A16B),
                            fontSize = 12.sp
                        )
                    },
                    selected = true,
                    onClick = { /* Already on home */ }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RequestPage,
                            contentDescription = "Booking Requests"
                        )
                    },
                    label = {
                        Text(
                            "Requests",
                            fontSize = 12.sp
                        )
                    },
                    selected = false,
                    onClick = { onNavigateToBookingManagement() }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chats"
                        )
                    },
                    label = {
                        Text(
                            "Chats",
                            fontSize = 12.sp
                        )
                    },
                    selected = false,
                    onClick = { onNavigateToChats() }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Your rides"
                        )
                    },
                    label = {
                        Text(
                            "Your rides",
                            fontSize = 12.sp
                        )
                    },
                    selected = false,
                    onClick = { onNavigateToYourRides() }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = {
                        Text(
                            "Profile",
                            fontSize = 12.sp
                        )
                    },
                    selected = false,
                    onClick = { onNavigateToProfile() }
                )
            }
        }

        // My location button
        FloatingActionButton(
            onClick = {
                userLocation?.let { location ->
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(location, 15f))
                } ?: run {
                    locationViewModel.getLastLocation()
                }
            },
            containerColor = Color.White,
            contentColor = Color(0xFF00A16B),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 240.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location"
            )
        }

        // Available trips button (replaced zoom in button)
        FloatingActionButton(
            onClick = {
                // Set default from location to current location
                tripSearchFromLocation = "Current Location"
                userLocation?.let {
                    tripSearchFromLat = it.latitude
                    tripSearchFromLng = it.longitude
                }
                // Clear destination
                tripSearchToLocation = ""
                tripSearchToAddress = ""
                tripSearchToLat = 0.0
                tripSearchToLng = 0.0

                // Show trips list
                showTripsList = true

                // Load popular trips
                searchViewModel.loadPopularTrips()
            },
            containerColor = Color(0xFF00A16B),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 290.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Available Trips",
                modifier = Modifier.size(24.dp)
            )
        }

        // Trips count badge
        if (searchResults.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 330.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${searchResults.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Trips List Dialog
    if (showTripsList) {
        Dialog(
            onDismissRequest = { showTripsList = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Trips",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showTripsList = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    Divider()

                    // Location inputs
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // From location
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSelectingFromLocation = true
                                    showLocationSelector = true
                                },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF00A16B),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "From",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = tripSearchFromLocation,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (tripSearchFromAddress.isNotBlank()) {
                                        Text(
                                            text = tripSearchFromAddress,
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // To location
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSelectingFromLocation = false
                                    showLocationSelector = true
                                },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "To",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = tripSearchToLocation.ifBlank { "Select destination" },
                                        fontWeight = FontWeight.Medium,
                                        color = if (tripSearchToLocation.isBlank()) Color.Gray else Color.Black
                                    )
                                    if (tripSearchToAddress.isNotBlank()) {
                                        Text(
                                            text = tripSearchToAddress,
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Search button
                        Button(
                            onClick = {
                                if (tripSearchToLocation.isNotBlank()) {
                                    searchViewModel.searchTrips(
                                        fromQuery = tripSearchFromLocation,
                                        toQuery = tripSearchToLocation,
                                        passengerCount = 1
                                    )
                                } else {
                                    Toast.makeText(context, "Please select a destination", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            enabled = tripSearchToLocation.isNotBlank() && !isSearching,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A16B)
                            )
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Search Trips")
                            }
                        }
                    }

                    Divider()

                    // Results or empty state
                    if (searchResults.isEmpty() && !isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No trips found",
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Select a destination to find available trips",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        // Trip results
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults) { result ->
                                TripCard(
                                    searchResult = result,
                                    onClick = {
                                        showTripsList = false
                                        onNavigateToRideDetails(result.trip.tripId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Location selector dialog
    if (showLocationSelector) {
        Dialog(
            onDismissRequest = { showLocationSelector = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                LocationSelectionScreen(
                    initialQuery = if (isSelectingFromLocation) {
                        if (tripSearchFromLocation == "Current Location") "" else tripSearchFromLocation
                    } else {
                        tripSearchToLocation
                    },
                    onLocationSelected = { locationData ->
                        if (isSelectingFromLocation) {
                            tripSearchFromLocation = locationData.name
                            tripSearchFromAddress = locationData.address
                            tripSearchFromLat = locationData.latLng.latitude
                            tripSearchFromLng = locationData.latLng.longitude
                        } else {
                            tripSearchToLocation = locationData.name
                            tripSearchToAddress = locationData.address
                            tripSearchToLat = locationData.latLng.latitude
                            tripSearchToLng = locationData.latLng.longitude
                        }
                        showLocationSelector = false
                    },
                    onDismiss = { showLocationSelector = false },
                    locationViewModel = locationViewModel
                )
            }
        }
    }
}

@Composable
fun TripCard(
    searchResult: TripSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Driver info and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${searchResult.driverProfile.firstName} ${searchResult.driverProfile.lastName}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format("%.1f", searchResult.driverProfile.rating),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        if (searchResult.car != null) {
                            Text(
                                text = " • ${searchResult.car.make} ${searchResult.car.model}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Text(
                    text = "${String.format("%.2f", searchResult.trip.price)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A16B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // From
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF00A16B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = searchResult.route.startLocation,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp),
                            maxLines = 1
                        )
                    }

                    // To
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = searchResult.route.endLocation,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(searchResult.trip.departureTime)),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = "Seats",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${searchResult.trip.availableSeats} seats",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}