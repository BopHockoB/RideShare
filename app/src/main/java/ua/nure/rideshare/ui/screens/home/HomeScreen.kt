package ua.nure.rideshare.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest
import ua.nure.rideshare.R
import ua.nure.rideshare.ui.viewmodels.HomeViewModel
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import ua.nure.rideshare.ui.viewmodels.NearbyLocationsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    locationViewModel: LocationViewModel,
    homeViewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSearch: (String, String) -> Unit,
    onNavigateToRideDetails: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToYourRides: () -> Unit,
    onNavigateToCreateRide: () -> Unit
) {
    // UI state
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Rider") }

    // Get context for Toast
    val context = LocalContext.current

    // Collect user location from LocationViewModel
    val userLocation by locationViewModel.userLocation.collectAsState(initial = null)
    val isLocationLoading by locationViewModel.isLoading.collectAsState()

    // Collect trips from HomeViewModel
    val availableTrips by homeViewModel.availableTrips.collectAsState()
    val nearbyLocationsState by homeViewModel.nearbyLocationsState.collectAsState()

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
                    // Search bar
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
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = Color.Gray
                            )
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
                            .clickable {
                                // Pass current location if available
                                userLocation?.let { location ->
                                    onNavigateToSearch(
                                        "${location.latitude},${location.longitude}",
                                        ""
                                    )
                                } ?: onNavigateToSearch("", "")
                            }
                    )

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

        // Zoom in button
        FloatingActionButton(
            onClick = {
                cameraPositionState.move(
                    CameraUpdateFactory.zoomIn()
                )
            },
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 290.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In"
            )
        }

        // Zoom out button
        FloatingActionButton(
            onClick = {
                cameraPositionState.move(
                    CameraUpdateFactory.zoomOut()
                )
            },
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 340.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out"
            )
        }
    }
}
