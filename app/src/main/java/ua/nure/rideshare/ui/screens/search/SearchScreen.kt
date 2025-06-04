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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ua.nure.rideshare.ui.screens.location.LocationData
import ua.nure.rideshare.ui.screens.location.LocationSelectionScreen
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import ua.nure.rideshare.ui.viewmodels.SearchViewModel
import ua.nure.rideshare.ui.viewmodels.TripSearchResult
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
    searchViewModel: SearchViewModel = hiltViewModel()
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

    // Collect data from ViewModels
    val isSearching by searchViewModel.isLoading.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val errorMessage by searchViewModel.errorMessage.collectAsState()
    val userLocation by locationViewModel.userLocation.collectAsState()

    var hasSearched by remember { mutableStateOf(false) }

    // Update from location with user's current location
    LaunchedEffect(userLocation) {
        if (fromLocation == "Current Location" && userLocation != null) {
            fromLatitude = userLocation!!.latitude
            fromLongitude = userLocation!!.longitude
            fromAddress = "Your current location"
        }
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            searchViewModel.clearError()
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
                    onSwapLocations = {
                        // Swap locations
                        val tempLocation = fromLocation
                        val tempAddress = fromAddress
                        val tempLat = fromLatitude
                        val tempLng = fromLongitude

                        fromLocation = toLocation
                        fromAddress = toAddress
                        fromLatitude = toLatitude
                        fromLongitude = toLongitude

                        toLocation = tempLocation
                        toAddress = tempAddress
                        toLatitude = tempLat
                        toLongitude = tempLng
                    },
                    onSearchClick = {
                        if (toLocation != "Where to?" && toLocation.isNotBlank()) {
                            hasSearched = true
                            searchViewModel.searchTrips(
                                fromQuery = fromLocation,
                                toQuery = toLocation,
                                departureDate = selectedDate?.time,
                                passengerCount = minSeats,
                                maxPrice = maxPrice.toDouble()
                            )
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

            // Search Results or Popular Routes
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

                if (searchResults.isNotEmpty()) {
                    items(searchResults) { result ->
                        TripResultCard(
                            searchResult = result,
                            onClick = { onNavigateToRideDetails(result.trip.tripId) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else if (!isSearching) {
                    item {
                        EmptySearchResults()
                    }
                }
            } else {
                // Show popular trips when no search has been made
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Popular Routes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Load popular trips on first load

                    searchViewModel.loadPopularTrips()


                if (searchResults.isNotEmpty()) {
                    items(searchResults) { result ->
                        TripResultCard(
                            searchResult = result,
                            onClick = {
                                // Pre-fill search with this route
                                fromLocation = result.route.startLocation
                                fromAddress = result.route.startAddress
                                fromLatitude = result.route.startLatitude
                                fromLongitude = result.route.startLongitude

                                toLocation = result.route.endLocation
                                toAddress = result.route.endAddress
                                toLatitude = result.route.endLatitude
                                toLongitude = result.route.endLongitude

                                // Navigate to details
                                onNavigateToRideDetails(result.trip.tripId)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                    .fillMaxHeight(0.9f)
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
                    .fillMaxHeight(0.9f)
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.time ?: System.currentTimeMillis()
        )

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
    onSwapLocations: () -> Unit,
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
                iconTint = Color(0xFF00A16B),
                onClick = onFromLocationClick
            )

            // Swap Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onSwapLocations,
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

            // To Location
            LocationInput(
                label = "To",
                location = toLocation,
                address = toAddress,
                iconTint = Color.Red,
                onClick = onToLocationClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            OutlinedTextField(
                value = selectedDate?.let { dateFormatter.format(it) } ?: "Today",
                onValueChange = { },
                label = { Text("Date") },
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
                        IconButton(onClick = { /* Clear date - implement if needed */ }) {
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
                    .clickable(enabled = !isSearching) { onDateClick() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray
                ),
                enabled = !isSearching
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
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
            if (address.isNotBlank() && location != address && address != "Use your current location") {
                Text(
                    text = address,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Select",
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
                    text = "$${String.format("%.2f", searchResult.trip.price)}",
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

                Text(
                    text = "${searchResult.route.distance.toInt()} km",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            if (!searchResult.trip.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = searchResult.trip.notes,
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