package ua.nure.rideshare.ui.screens.booking

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.ui.screens.location.LocationSelectionScreen
import ua.nure.rideshare.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingRequestScreen(
    tripId: String,
    userId: String,
    trip: Trip?,
    route: Route?,
    driverProfile: Profile?,
    car: Car?,
    onBackClick: () -> Unit,
    onBookingCreated: () -> Unit,
    locationViewModel: LocationViewModel,
    bookingViewModel: BookingRequestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Form state
    var selectedSeats by remember { mutableStateOf(1) }
    var pickupLocation by remember { mutableStateOf("") }
    var pickupAddress by remember { mutableStateOf("") }
    var pickupLatitude by remember { mutableStateOf<Double?>(null) }
    var pickupLongitude by remember { mutableStateOf<Double?>(null) }
    var dropoffLocation by remember { mutableStateOf("") }
    var dropoffAddress by remember { mutableStateOf("") }
    var dropoffLatitude by remember { mutableStateOf<Double?>(null) }
    var dropoffLongitude by remember { mutableStateOf<Double?>(null) }
    var message by remember { mutableStateOf("") }

    // UI state
    var showPickupLocationSelector by remember { mutableStateOf(false) }
    var showDropoffLocationSelector by remember { mutableStateOf(false) }
    var bookingEligibility by remember { mutableStateOf<BookingEligibility?>(null) }

    // ViewModel states
    val isSubmitting by bookingViewModel.isSubmitting.collectAsState()
    val errorMessage by bookingViewModel.errorMessage.collectAsState()

    // Get user location
    val userLocation by locationViewModel.userLocation.collectAsState()

    // Initialize pickup location with user's current location
    LaunchedEffect(userLocation) {
        if (pickupLocation.isEmpty() && userLocation != null) {
            pickupLocation = "Current Location"
            pickupAddress = "Your current location"
            pickupLatitude = userLocation!!.latitude
            pickupLongitude = userLocation!!.longitude
        }
    }

    // Initialize with route endpoints
    LaunchedEffect(route) {
        route?.let {
            if (dropoffLocation.isEmpty()) {
                dropoffLocation = it.endLocation
                dropoffAddress = it.endAddress
                dropoffLatitude = it.endLatitude
                dropoffLongitude = it.endLongitude
            }
        }
    }

    // Check booking eligibility
    LaunchedEffect(tripId, userId) {
        bookingEligibility = bookingViewModel.canUserBookTrip(tripId, userId)
    }

    // Handle booking events
    LaunchedEffect(Unit) {
        bookingViewModel.bookingEvent.collectLatest { event ->
            when (event) {
                is BookingEvent.BookingCreated -> {
                    Toast.makeText(context, "Booking request sent successfully!", Toast.LENGTH_SHORT).show()
                    onBookingCreated()
                }
                is BookingEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            bookingViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Book This Ride") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                .verticalScroll(scrollState)
        ) {
            // Check eligibility first
            when (val eligibility = bookingEligibility) {
                is BookingEligibility.Eligible -> {
                    // Show booking form
                    BookingForm(
                        trip = trip,
                        route = route,
                        driverProfile = driverProfile,
                        car = car,
                        selectedSeats = selectedSeats,
                        onSeatsChanged = { selectedSeats = it },
                        pickupLocation = pickupLocation,
                        pickupAddress = pickupAddress,
                        onPickupLocationClick = { showPickupLocationSelector = true },
                        dropoffLocation = dropoffLocation,
                        dropoffAddress = dropoffAddress,
                        onDropoffLocationClick = { showDropoffLocationSelector = true },
                        message = message,
                        onMessageChanged = { message = it },
                        isSubmitting = isSubmitting,
                        onSubmitBooking = {
                            coroutineScope.launch {
                                val request = CreateBookingRequest(
                                    tripId = tripId,
                                    passengerId = userId,
                                    seats = selectedSeats,
                                    pickupLocation = pickupLocation.takeIf { it.isNotBlank() },
                                    pickupLatitude = pickupLatitude,
                                    pickupLongitude = pickupLongitude,
                                    dropoffLocation = dropoffLocation.takeIf { it.isNotBlank() },
                                    dropoffLatitude = dropoffLatitude,
                                    dropoffLongitude = dropoffLongitude,
                                    message = message.takeIf { it.isNotBlank() }
                                )
                                bookingViewModel.createBookingRequest(request)
                            }
                        }
                    )
                }

                is BookingEligibility.AlreadyBooked -> {
                    AlreadyBookedMessage(status = eligibility.status)
                }

                is BookingEligibility.CannotBookOwnTrip -> {
                    ErrorMessage("You cannot book your own trip")
                }

                is BookingEligibility.TripNotAvailable -> {
                    ErrorMessage("This trip is no longer available for booking")
                }

                is BookingEligibility.NoSeatsAvailable -> {
                    ErrorMessage("No seats available for this trip")
                }

                is BookingEligibility.TripNotFound -> {
                    ErrorMessage("Trip not found")
                }

                is BookingEligibility.Error -> {
                    ErrorMessage(eligibility.message)
                }

                null -> {
                    // Loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00A16B))
                    }
                }
            }
        }
    }

    // Location selection dialogs
    if (showPickupLocationSelector) {
        LocationSelectionDialog(
            title = "Select Pickup Location",
            initialQuery = pickupLocation,
            onLocationSelected = { locationData ->
                pickupLocation = locationData.name
                pickupAddress = locationData.address
                pickupLatitude = locationData.latLng.latitude
                pickupLongitude = locationData.latLng.longitude
                showPickupLocationSelector = false
            },
            onDismiss = { showPickupLocationSelector = false },
            locationViewModel = locationViewModel
        )
    }

    if (showDropoffLocationSelector) {
        LocationSelectionDialog(
            title = "Select Dropoff Location",
            initialQuery = dropoffLocation,
            onLocationSelected = { locationData ->
                dropoffLocation = locationData.name
                dropoffAddress = locationData.address
                dropoffLatitude = locationData.latLng.latitude
                dropoffLongitude = locationData.latLng.longitude
                showDropoffLocationSelector = false
            },
            onDismiss = { showDropoffLocationSelector = false },
            locationViewModel = locationViewModel
        )
    }
}

@Composable
fun BookingForm(
    trip: Trip?,
    route: Route?,
    driverProfile: Profile?,
    car: Car?,
    selectedSeats: Int,
    onSeatsChanged: (Int) -> Unit,
    pickupLocation: String,
    pickupAddress: String,
    onPickupLocationClick: () -> Unit,
    dropoffLocation: String,
    dropoffAddress: String,
    onDropoffLocationClick: () -> Unit,
    message: String,
    onMessageChanged: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmitBooking: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Trip Summary Card
        trip?.let { tripData ->
            TripSummaryCard(
                trip = tripData,
                route = route,
                driverProfile = driverProfile,
                car = car,
                dateFormatter = dateFormatter
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seat Selection
        SeatSelectionCard(
            selectedSeats = selectedSeats,
            maxSeats = trip?.availableSeats ?: 1,
            pricePerSeat = trip?.price ?: 0.0,
            onSeatsChanged = onSeatsChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pickup Location
        LocationCard(
            title = "Pickup Location",
            location = pickupLocation,
            address = pickupAddress,
            iconTint = Color.Red,
            onClick = onPickupLocationClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dropoff Location
        LocationCard(
            title = "Dropoff Location",
            location = dropoffLocation,
            address = dropoffAddress,
            iconTint = Color(0xFF00A16B),
            onClick = onDropoffLocationClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message to Driver
        MessageCard(
            message = message,
            onMessageChanged = onMessageChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = onSubmitBooking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSubmitting && selectedSeats > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A16B)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending Request...")
            } else {
                val totalPrice = (trip?.price ?: 0.0) * selectedSeats
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Send Request - ${String.format("%.2f", totalPrice)}")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TripSummaryCard(
    trip: Trip,
    route: Route?,
    driverProfile: Profile?,
    car: Car?,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Trip Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Route information
            route?.let { routeInfo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )

                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF00A16B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = routeInfo.startLocation,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = routeInfo.endLocation,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${routeInfo.distance} km",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${routeInfo.duration} min",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            // Trip info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Departure",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = dateFormatter.format(Date(trip.departureTime)),
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available Seats",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${trip.availableSeats}",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Driver and car info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver info
                driverProfile?.let { driver ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00A16B).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = driver.firstName.first().toString() + driver.lastName.first().toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A16B)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "${driver.firstName} ${driver.lastName}",
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${driver.rating} (${driver.tripsCount} trips)",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Car info
                car?.let { vehicle ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${vehicle.make} ${vehicle.model}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = vehicle.color,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeatSelectionCard(
    selectedSeats: Int,
    maxSeats: Int,
    pricePerSeat: Double,
    onSeatsChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Number of Seats",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Seat selection
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (selectedSeats > 1) onSeatsChanged(selectedSeats - 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape),
                        enabled = selectedSeats > 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease seats",
                            tint = if (selectedSeats > 1) Color(0xFF00A16B) else Color.Gray
                        )
                    }

                    Text(
                        text = selectedSeats.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    IconButton(
                        onClick = { if (selectedSeats < maxSeats) onSeatsChanged(selectedSeats + 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape),
                        enabled = selectedSeats < maxSeats
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase seats",
                            tint = if (selectedSeats < maxSeats) Color(0xFF00A16B) else Color.Gray
                        )
                    }
                }

                // Price calculation
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.2f", pricePerSeat)} per seat",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Total: ${String.format("%.2f", pricePerSeat * selectedSeats)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A16B)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationCard(
    title: String,
    location: String,
    address: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                    text = title,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = location.ifBlank { "Select location" },
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
}

@Composable
fun MessageCard(
    message: String,
    onMessageChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Message to Driver (Optional)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = message,
                onValueChange = onMessageChanged,
                placeholder = { Text("Let the driver know any special requests...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun AlreadyBookedMessage(status: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF856404),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Already Booked",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF856404)
            )

            Text(
                text = "You already have a booking for this trip.",
                fontSize = 14.sp,
                color = Color(0xFF856404),
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Status: ${status.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF856404),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8D7DA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFF721C24),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cannot Book",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF721C24)
            )

            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF721C24),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun LocationSelectionDialog(
    title: String,
    initialQuery: String,
    onLocationSelected: (ua.nure.rideshare.ui.screens.location.LocationData) -> Unit,
    onDismiss: () -> Unit,
    locationViewModel: LocationViewModel
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LocationSelectionScreen(
                initialQuery = initialQuery,
                onLocationSelected = onLocationSelected,
                onDismiss = onDismiss,
                locationViewModel = locationViewModel
            )
        }
    }
}