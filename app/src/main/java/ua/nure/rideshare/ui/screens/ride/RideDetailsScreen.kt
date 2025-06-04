package ua.nure.rideshare.ui.screens.ride

import android.widget.Toast
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailsScreen(
    rideId: String,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit, // Keep the original signature
    rideViewModel: RideViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    bookingViewModel: BookingRequestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Get current user ID from AuthViewModel
    val currentUserId by authViewModel.currentUserId.collectAsState()

    // Trip data states
    var trip by remember { mutableStateOf<Trip?>(null) }
    var route by remember { mutableStateOf<Route?>(null) }
    var driverProfile by remember { mutableStateOf<Profile?>(null) }
    var car by remember { mutableStateOf<Car?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var bookingEligibility by remember { mutableStateOf<BookingEligibility?>(null) }

    // Set current user ID in ViewModels
    LaunchedEffect(currentUserId) {
        currentUserId?.let { userId ->
            rideViewModel.setCurrentUserId(userId)
            bookingViewModel.setCurrentUserId(userId)
        }
    }

    // Load trip details
    LaunchedEffect(rideId) {
        isLoading = true
        error = null

        try {
            // Get trip with its route
            val (tripData, routeData) = rideViewModel.getTripWithRoute(rideId)

            if (tripData != null && routeData != null) {
                trip = tripData
                route = routeData

                // Load driver profile and car from search results
                searchViewModel.loadPopularTrips()

                // Wait for search results and find our trip
                searchViewModel.searchResults.collect { results ->
                    val matchingResult = results.find { it.trip.tripId == rideId }
                    if (matchingResult != null) {
                        driverProfile = matchingResult.driverProfile
                        car = matchingResult.car

                        // Check booking eligibility
                        currentUserId?.let { userId ->
                            bookingEligibility = bookingViewModel.canUserBookTrip(rideId, userId)
                        }

                        isLoading = false
                        return@collect
                    }
                }
            } else {
                error = "Trip not found"
                isLoading = false
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load trip details"
            isLoading = false
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
                    // Add bookmark action
                    IconButton(onClick = { /* Handle bookmark */ }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00A16B))
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error!!,
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A16B)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }

            trip != null && route != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    // Route Card
                    TripRouteCard(
                        trip = trip!!,
                        route = route!!
                    )

                    // Driver Card
                    if (driverProfile != null) {
                        DriverAndCarCard(
                            driverProfile = driverProfile!!,
                            car = car
                        )
                    }

                    // Trip Details Card
                    TripDetailsCard(
                        trip = trip!!,
                        car = car
                    )

                    // Notes Card
                    if (!trip!!.notes.isNullOrBlank()) {
                        NotesCard(notes = trip!!.notes!!)
                    }

                    // Bottom padding for button
                    Spacer(modifier = Modifier.height(100.dp))
                }

                // Floating action button for booking
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BookingActionButton(
                        trip = trip!!,
                        currentUserId = currentUserId,
                        bookingEligibility = bookingEligibility,
                        onBookClick = onBookClick // Use the original callback
                    )
                }
            }
        }
    }
}

@Composable
fun TripRouteCard(
    trip: Trip,
    route: Route
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Route visualization
            Row(
                modifier = Modifier.fillMaxWidth()
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
                            .height(60.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
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
                    // From location
                    Column(
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "From",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = route.startLocation,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Text(
                            text = route.startAddress,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // To location
                    Column {
                        Text(
                            text = "To",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = route.endLocation,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Text(
                            text = route.endAddress,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            // Trip details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Departure time
                TripDetailItem(
                    icon = Icons.Default.Schedule,
                    title = "Departure",
                    value = dateFormatter.format(Date(trip.departureTime))
                )

                // Distance
                TripDetailItem(
                    icon = Icons.Default.Navigation,
                    title = "Distance",
                    value = "${route.distance.toInt()} km"
                )

                // Duration
                TripDetailItem(
                    icon = Icons.Default.Timer,
                    title = "Duration",
                    value = "${route.duration} min"
                )
            }
        }
    }
}

@Composable
fun DriverAndCarCard(
    driverProfile: Profile,
    car: Car?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Driver",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Driver avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A16B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = driverProfile.firstName.first().toString() +
                                driverProfile.lastName.first().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A16B)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "${driverProfile.firstName} ${driverProfile.lastName}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format("%.1f", driverProfile.rating),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Text(
                            text = " • ${driverProfile.tripsCount} trips",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    if (!driverProfile.bio.isNullOrBlank()) {
                        Text(
                            text = driverProfile.bio,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2
                        )
                    }
                }

                // Contact button
                IconButton(
                    onClick = { /* Handle contact driver */ },
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

            // Car information
            car?.let { vehicle ->
                Divider(
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF00A16B),
                        modifier = Modifier.size(24.dp)
                    )

                    Column(
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = "${vehicle.make} ${vehicle.model} (${vehicle.year})",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${vehicle.color} • ${vehicle.licensePlate} • ${vehicle.seats} seats",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Car amenities
                if (!vehicle.amenities.isNullOrBlank()) {
                    val amenitiesList = vehicle.amenities.split(",")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amenitiesList.take(3).forEach { amenity ->
                            Surface(
                                modifier = Modifier,
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x1A00A16B)
                            ) {
                                Text(
                                    text = amenity.trim(),
                                    color = Color(0xFF00A16B),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (amenitiesList.size > 3) {
                            Text(
                                text = "+${amenitiesList.size - 3} more",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripDetailsCard(
    trip: Trip,
    car: Car?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Trip Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Price
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$${String.format("%.2f", trip.price)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A16B)
                    )
                    Text(
                        text = "per seat",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )

                // Available seats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${trip.availableSeats}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "seats left",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )

                // Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = trip.status.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (trip.status) {
                            "SCHEDULED" -> Color(0xFF00A16B)
                            "IN_PROGRESS" -> Color(0xFFFF9800)
                            "COMPLETED" -> Color(0xFF4CAF50)
                            "CANCELLED" -> Color(0xFFF44336)
                            else -> Color.Gray
                        }
                    )
                    Text(
                        text = "status",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun NotesCard(notes: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Note,
                    contentDescription = null,
                    tint = Color(0xFF00A16B),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Notes from driver",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = notes,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TripDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun BookingActionButton(
    trip: Trip,
    currentUserId: String?,
    bookingEligibility: BookingEligibility?,
    onBookClick: () -> Unit // Keep original signature
) {
    val context = LocalContext.current

    val (buttonText, buttonColor, isEnabled) = when {
        currentUserId == null -> Triple("Login to Book", Color.Gray, false)
        currentUserId == trip.driverId -> Triple("This is Your Trip", Color.Gray, false)
        trip.status != "SCHEDULED" -> Triple("Trip Not Available", Color.Gray, false)
        trip.availableSeats <= 0 -> Triple("No Seats Available", Color.Gray, false)
        bookingEligibility is BookingEligibility.AlreadyBooked ->
            Triple("Already Requested (${bookingEligibility.status})", Color.Gray, false)
        else -> Triple("Request to Book", Color(0xFF00A16B), true)
    }

    Button(
        onClick = {
            when {
                currentUserId == null -> {
                    Toast.makeText(context, "Please log in to book a ride", Toast.LENGTH_SHORT).show()
                }
                currentUserId == trip.driverId -> {
                    Toast.makeText(context, "You cannot book your own ride", Toast.LENGTH_SHORT).show()
                }
                !isEnabled -> {
                    Toast.makeText(context, "This trip is not available for booking", Toast.LENGTH_SHORT).show()
                }
                else -> onBookClick() // Call the original callback
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Default.Send else Icons.Default.Block,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = buttonText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}