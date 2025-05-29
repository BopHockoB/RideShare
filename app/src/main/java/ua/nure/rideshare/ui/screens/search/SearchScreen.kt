package ua.nure.rideshare.ui.screens.search

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.ui.screens.location.LocationData
import ua.nure.rideshare.ui.screens.location.LocationSelectionScreen
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import ua.nure.rideshare.ui.viewmodels.RideViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    locationViewModel: LocationViewModel,
    origin: String = "",
    destination: String = "",
    onNavigateToRideDetails: (String) -> Unit,
    onBackClick: () -> Unit,
    rideViewModel: RideViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Search state
    var fromLocation by remember { mutableStateOf(origin.ifBlank { "Current Location" }) }
    var fromAddress by remember { mutableStateOf("Use your current location") }
    var fromLatitude by remember { mutableStateOf(0.0) }
    var fromLongitude by remember { mutableStateOf(0.0) }

    var toLocation by remember { mutableStateOf(destination.ifBlank { "Where to?" }) }
    var toAddress by remember { mutableStateOf("") }
    var toLatitude by remember { mutableStateOf(0.0) }
    var toLongitude by remember { mutableStateOf(0.0) }

    // Filter state
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var maxPrice by remember { mutableStateOf(50f) }
    var minSeats by remember { mutableStateOf(1) }
    var showFilters by remember { mutableStateOf(false) }

    // UI state
    var showFromLocationSelector by remember { mutableStateOf(false) }
    var showToLocationSelector by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    // Get user location
    val userLocation by locationViewModel.userLocation.collectAsState()

    // Update from location with user's current location
    LaunchedEffect(userLocation) {
        if (fromLocation == "Current Location" && userLocation != null) {
            fromLatitude = userLocation!!.latitude
            fromLongitude = userLocation!!.longitude
            fromAddress = "Your current location"
        }
    }

    // Date formatter
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Find a Ride") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filters"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Form
            item {
                SearchForm(
                    fromLocation = fromLocation,
                    fromAddress = fromAddress,
                    toLocation = toLocation,
                    toAddress = toAddress,
                    selectedDate = selectedDate,
                    dateFormatter = dateFormatter,
                    onFromLocationClick = { showFromLocationSelector = true },
                    onToLocationClick = { showToLocationSelector = true },
                    onDateClick = { showDatePicker = true },
                    onSearchClick = {
                        if (toLocation != "Where to?" && toLocation.isNotBlank()) {
                            isSearching = true
                            coroutineScope.launch {
                                try {
                                    // Simulate search - in real app, you'd search by coordinates
                                    rideViewModel.loadAvailableTrips()
                                    // For now, get all available trips
                                    // In a real implementation, you'd filter by location, date, etc.
                                    searchResults = emptyList() // Placeholder
                                    hasSearched = true
                                    Toast.makeText(context, "Search completed", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSearching = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please select a destination", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isSearching = isSearching
                )
            }

            // Filters Section
            if (showFilters) {
                item {
                    FiltersSection(
                        maxPrice = maxPrice,
                        minSeats = minSeats,
                        onMaxPriceChange = { maxPrice = it },
                        onMinSeatsChange = { minSeats = it }
                    )
                }
            }

            // Search Results Header
            if (hasSearched) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (searchResults.isEmpty()) "No rides found" else "${searchResults.size} rides found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // Search Results
            if (searchResults.isNotEmpty()) {
                items(searchResults) { trip ->
                    TripResultCard(
                        trip = trip,
                        onClick = { onNavigateToRideDetails(trip.tripId) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else if (hasSearched && !isSearching) {
                // Empty state
                item {
                    EmptySearchResults()
                }
            }

            // Popular Routes (when no search has been made)
            if (!hasSearched) {
                item {
                    PopularRoutesSection(
                        onRouteClick = { route ->
                            toLocation = route.destination
                            toAddress = route.address
                            toLatitude = route.latitude
                            toLongitude = route.longitude
                        }
                    )
                }
            }
        }
    }

    // Location Selection Dialogs
    if (showFromLocationSelector) {
        Dialog(
            onDismissRequest = { showFromLocationSelector = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                LocationSelectionScreen(
                    initialQuery = if (fromLocation == "Current Location") "" else fromLocation,
                    onLocationSelected = { locationData ->
                        fromLocation = locationData.name
                        fromAddress = locationData.address
                        fromLatitude = locationData.latLng.latitude
                        fromLongitude = locationData.latLng.longitude
                        showFromLocationSelector = false
                    },
                    onDismiss = { showFromLocationSelector = false },
                    locationViewModel = locationViewModel
                )
            }
        }
    }

    if (showToLocationSelector) {
        Dialog(
            onDismissRequest = { showToLocationSelector = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                LocationSelectionScreen(
                    initialQuery = if (toLocation == "Where to?") "" else toLocation,
                    onLocationSelected = { locationData ->
                        toLocation = locationData.name
                        toAddress = locationData.address
                        toLatitude = locationData.latLng.latitude
                        toLongitude = locationData.latLng.longitude
                        showToLocationSelector = false
                    },
                    onDismiss = { showToLocationSelector = false },
                    locationViewModel = locationViewModel
                )
            }
        }
    }

    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SearchForm(
    fromLocation: String,
    fromAddress: String,
    toLocation: String,
    toAddress: String,
    selectedDate: Date?,
    dateFormatter: SimpleDateFormat,
    onFromLocationClick: () -> Unit,
    onToLocationClick: () -> Unit,
    onDateClick: () -> Unit,
    onSearchClick: () -> Unit,
    isSearching: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // From Location
            LocationInput(
                label = "From",
                location = fromLocation,
                address = fromAddress,
                iconTint = Color.Red,
                onClick = onFromLocationClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Swap Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { /* Implement location swap */ },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap locations",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // To Location
            LocationInput(
                label = "To",
                location = toLocation,
                address = toAddress,
                iconTint = Color(0xFF00A16B),
                onClick = onToLocationClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            OutlinedTextField(
                value = selectedDate?.let { dateFormatter.format(it) } ?: "",
                onValueChange = { },
                label = { Text("Date (optional)") },
                readOnly = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = Color(0xFF00A16B)
                    )
                },
                trailingIcon = {
                    if (selectedDate != null) {
                        IconButton(onClick = { /* Clear date */ }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear date",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Button
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSearching,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Searching...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Search Rides")
                }
            }
        }
    }
}

@Composable
fun LocationInput(
    label: String,
    location: String,
    address: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = location,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (address.isNotBlank() && location != address) {
                Text(
                    text = address,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection(
    maxPrice: Float,
    minSeats: Int,
    onMaxPriceChange: (Float) -> Unit,
    onMinSeatsChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filters",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Max Price Filter
            Text(
                text = "Max Price: $${maxPrice.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = maxPrice,
                onValueChange = onMaxPriceChange,
                valueRange = 5f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00A16B),
                    activeTrackColor = Color(0xFF00A16B)
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Min Seats Filter
            Text(
                text = "Minimum Seats: $minSeats",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..4).forEach { seats ->
                    FilterChip(
                        onClick = { onMinSeatsChange(seats) },
                        label = { Text("$seats") },
                        selected = minSeats == seats,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00A16B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TripResultCard(
    trip: Trip,
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
            // Trip header with price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Ride",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$${String.format("%.2f", trip.price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A16B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Departure: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(trip.departureTime))}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${trip.availableSeats} seats",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            if (!trip.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = trip.notes,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun EmptySearchResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No rides found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        Text(
            text = "Try adjusting your search criteria",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun PopularRoutesSection(
    onRouteClick: (PopularRoute) -> Unit
) {
    val popularRoutes = remember {
        listOf(
            PopularRoute("Downtown", "123 Main St, Downtown", 40.7589, -73.9851),
            PopularRoute("Airport", "JFK Airport, Queens", 40.6413, -73.7781),
            PopularRoute("University", "State University Campus", 40.7282, -73.9942),
            PopularRoute("Mall", "Shopping Center Plaza", 40.7505, -73.9934)
        )
    }

    Column(
        modifier = Modifier.padding(top = 24.dp)
    ) {
        Text(
            text = "Popular Destinations",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        popularRoutes.forEach { route ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onRouteClick(route) },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF00A16B),
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Column {
                        Text(
                            text = route.destination,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = route.address,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

data class PopularRoute(
    val destination: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)