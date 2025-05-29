package ua.nure.rideshare.ui.screens.ride

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.ui.viewmodels.RideViewModel
import ua.nure.rideshare.ui.viewmodels.AuthViewModel
import ua.nure.rideshare.ui.viewmodels.CarViewModel
import ua.nure.rideshare.ui.viewmodels.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailsScreen(
    rideId: String,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit,
    rideViewModel: RideViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    carViewModel: CarViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Get current user ID
    val currentUserId by authViewModel.currentUserId.collectAsState()
    Log.d("RIDE_DETAILS_SCREEN", "Current user id: $currentUserId")

    // Collect trip data directly
    val trip by rideViewModel.getTripById(rideId).collectAsState(initial = null)

    // State for other data
    var route by remember { mutableStateOf<Route?>(null) }
    var car by remember { mutableStateOf<Car?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load route when trip changes
    LaunchedEffect(trip?.routeId) {
        trip?.routeId?.let { routeId ->
            try {
                route = rideViewModel.getRouteByIdOnce(routeId)
                Log.d("RIDE_DETAILS_SCREEN", "Route loaded: ${route?.startLocation} to ${route?.endLocation}")
            } catch (e: Exception) {
                Log.e("RIDE_DETAILS_SCREEN", "Error loading route", e)
            }
        }
    }

    // Load driver profile when trip changes
    LaunchedEffect(trip?.driverId) {
        trip?.driverId?.let { driverId ->
            profileViewModel.loadProfile(driverId)
            Log.d("RIDE_DETAILS_SCREEN", "Loading profile for driver: $driverId")
        }
    }

    // Collect driver profile
    val driverProfile by profileViewModel.profile.collectAsState()

    // Load car data when trip changes
    LaunchedEffect(trip?.carId) {
        car = trip?.carId?.let { carViewModel.getCarById(it) }
    }

    // Date formatters
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Show error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            errorMessage = null // Clear after showing
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Ride Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add to favorites */ }) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                trip == null -> {
                    // Loading state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF00A16B)
                    )
                }

                else -> {
                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // Trip Route Card
                        route?.let { routeInfo ->
                            TripRouteCard(
                                route = routeInfo,
                                trip = trip!!,
                                dateFormatter = dateFormatter,
                                timeFormatter = timeFormatter
                            )
                        } ?: run {
                            // Route loading placeholder
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00A16B),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Driver and Vehicle Info Card
                        DriverVehicleCard(
                            driverProfile = driverProfile,
                            car = car,
                            onContactDriver = {
                                Toast.makeText(context, "Contact driver feature coming soon", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // Ride Details Card
                        RideDetailsCard(
                            trip = trip!!,
                            car = car
                        )

                        // Bottom padding for button
                        Spacer(modifier = Modifier.height(100.dp))
                    }

                    // Booking button (floating at bottom)
                    BookingButton(
                        trip = trip!!,
                        currentUserId = currentUserId,
                        isBooking = isBooking,
                        onBookClick = {
                            if (currentUserId != null && currentUserId != trip!!.driverId && trip!!.status == "SCHEDULED") {
                                isBooking = true
                                coroutineScope.launch {
                                    try {
                                        rideViewModel.bookTrip(
                                            tripId = trip!!.tripId,
                                            seats = 1 // Default to 1 seat, could be made configurable
                                        )
                                        Toast.makeText(context, "Ride booked successfully!", Toast.LENGTH_SHORT).show()
                                        onBookClick()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to book ride: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isBooking = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TripRouteCard(
    route: Route,
    trip: Trip,
    dateFormatter: SimpleDateFormat,
    timeFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Route with line connecting origin and destination
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Icons column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )

                    // Connecting line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(40.dp)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF00A16B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Location details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    // Origin
                    Column(
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = route.startLocation,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = route.startAddress,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Destination
                    Column {
                        Text(
                            text = route.endLocation,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = route.endAddress,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Divider(
                color = Color.LightGray,
                thickness = 1.dp
            )

            // Trip details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Distance
                TripDetail(
                    icon = Icons.Default.Straight,
                    title = "Distance",
                    value = "${route.distance} km"
                )

                // Duration
                TripDetail(
                    icon = Icons.Default.Timer,
                    title = "Duration",
                    value = "${route.duration} min"
                )

                // Departure
                TripDetail(
                    icon = Icons.Default.Schedule,
                    title = "Departure",
                    value = "${dateFormatter.format(Date(trip.departureTime))}, ${timeFormatter.format(Date(trip.departureTime))}"
                )
            }
        }
    }
}

@Composable
fun DriverVehicleCard(
    driverProfile: Profile?,
    car: Car?,
    onContactDriver: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Driver info
            driverProfile?.let { driver ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Driver photo
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder for driver photo
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
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
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = "${driver.rating} (${driver.tripsCount} trips)",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (!driver.bio.isNullOrBlank()) {
                            Text(
                                text = driver.bio,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 2
                            )
                        }
                    }

                    // Contact button
                    IconButton(
                        onClick = onContactDriver,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x1A00A16B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Contact Driver",
                            tint = Color(0xFF00A16B)
                        )
                    }
                }

                Divider(
                    color = Color.LightGray,
                    thickness = 1.dp
                )
            }

            // Vehicle info
            car?.let { vehicle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vehicle photo
                    Box(
                        modifier = Modifier
                            .size(80.dp, 50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder for vehicle photo
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = "${vehicle.make} ${vehicle.model}",
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "${vehicle.color}, ${vehicle.year}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "License: ${vehicle.licensePlate}",
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
fun RideDetailsCard(
    trip: Trip,
    car: Car?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Ride Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Available seats
            DetailRow(
                icon = Icons.Default.AirlineSeatReclineNormal,
                title = "Available Seats",
                value = "${trip.availableSeats}"
            )

            // Price
            DetailRow(
                icon = Icons.Default.AttachMoney,
                title = "Price",
                value = "$${String.format("%.2f", trip.price)}"
            )

            // Status
            DetailRow(
                icon = Icons.Default.Schedule,
                title = "Status",
                value = trip.status.lowercase().replaceFirstChar { it.uppercase() }
            )

            // Notes
            if (!trip.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = trip.notes,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Amenities
            car?.amenities?.let { amenitiesString ->
                if (amenitiesString.isNotBlank()) {
                    val amenitiesList = amenitiesString.split(",")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Amenities",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amenitiesList.forEach { amenity ->
                            Chip(
                                amenity = amenity.trim(),
                                onClick = { }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingButton(
    trip: Trip,
    currentUserId: String?,
    isBooking: Boolean,
    onBookClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val buttonText = when {
            currentUserId == null -> "Login to Book"
            currentUserId == trip.driverId -> "You are the driver"
            trip.status != "SCHEDULED" -> when (trip.status) {
                "IN_PROGRESS" -> "Ride in progress"
                "COMPLETED" -> "Ride completed"
                "CANCELLED" -> "Ride cancelled"
                else -> "Not available"
            }
            trip.availableSeats <= 0 -> "No seats available"
            isBooking -> "Booking..."
            else -> "Book this ride - $${String.format("%.2f", trip.price)}"
        }

        val isEnabled = currentUserId != null &&
                currentUserId != trip.driverId &&
                trip.status == "SCHEDULED" &&
                trip.availableSeats > 0 &&
                !isBooking

        Button(
            onClick = onBookClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A16B),
                disabledContainerColor = Color.Gray
            )
        ) {
            if (isBooking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = buttonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TripDetail(
    icon: ImageVector,
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00A16B),
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00A16B),
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun Chip(
    amenity: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x1A00A16B)
    ) {
        Text(
            text = amenity,
            color = Color(0xFF00A16B),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// Simple FlowRow implementation
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}