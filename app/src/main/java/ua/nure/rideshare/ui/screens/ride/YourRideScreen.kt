package ua.nure.rideshare.ui.screens.ride

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.data.model.TripBooking
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.ui.viewmodels.BookingRequestViewModel
import ua.nure.rideshare.ui.viewmodels.RideViewModel
import ua.nure.rideshare.ui.viewmodels.TripBookingViewModel
import java.text.SimpleDateFormat
import java.util.*

data class UserRideItem(
    val trip: Trip,
    val route: Route,
    val role: UserRole,
    val booking: TripBooking? = null,
    val driverProfile: Profile? = null,
    val passengerCount: Int = 0 // For driver trips
)

enum class UserRole {
    DRIVER,
    PASSENGER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourRideScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onCreateNewRide: () -> Unit,
    onNavigateToBookingManagement:() -> Unit,
    onRideDetails: (String) -> Unit,
    rideViewModel: RideViewModel = hiltViewModel(),
    bookingViewModel: TripBookingViewModel = hiltViewModel(),
    bookingRequestViewModel: BookingRequestViewModel = hiltViewModel()
) {
    // State
    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var driverTrips by remember { mutableStateOf<List<UserRideItem>>(emptyList()) }
    var passengerBookings by remember { mutableStateOf<List<UserRideItem>>(emptyList()) }


    // Tab titles with counts
    val tabTitles = remember(driverTrips, passengerBookings) {
        listOf(
            "As Driver (${driverTrips.size})",
            "As Passenger (${passengerBookings.size})"
        )
    }

    // Load data
    LaunchedEffect(userId) {
        isLoading = true
        try {
            // Load driver trips
            val trips = rideViewModel.getTripsByDriverId(userId).first()
            val driverItems = mutableListOf<UserRideItem>()

            for (trip in trips) {
                val route = rideViewModel.getRouteByIdOnce(trip.routeId)
                if (route != null) {
                    val bookings = bookingViewModel.getBookingsByTripIdOnce(trip.tripId)
                    val passengerCount = bookings
                        .filter { it.status in listOf("APPROVED", "PENDING") }
                        .sumOf { it.seats }

                    driverItems.add(
                        UserRideItem(
                            trip = trip,
                            route = route,
                            role = UserRole.DRIVER,
                            passengerCount = passengerCount
                        )
                    )
                }
            }
            driverTrips = driverItems.sortedByDescending { it.trip.departureTime }

            // Load passenger bookings
            val bookings = bookingViewModel.getBookingsByPassengerId(userId).first()
            val passengerItems = mutableListOf<UserRideItem>()

            for (booking in bookings) {
                val trip = rideViewModel.getTripByIdOnce(booking.tripId)
                val route = trip?.let { rideViewModel.getRouteByIdOnce(it.routeId) }
                val driverProfile = trip?.let { bookingViewModel.getProfileByIdOnce(it.driverId) }

                if (trip != null && route != null) {
                    passengerItems.add(
                        UserRideItem(
                            trip = trip,
                            route = route,
                            role = UserRole.PASSENGER,
                            booking = booking,
                            driverProfile = driverProfile
                        )
                    )
                }
            }
            passengerBookings = passengerItems.sortedByDescending { it.trip.departureTime }

        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Your Rides") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {

                    IconButton(onClick = onNavigateToBookingManagement) {
                        BadgedBox(
                            badge = {
//                                if (pendingRequestsCount > 0) {
//                                    Badge {
//                                        Text(
//                                            text = pendingRequestsCount.toString(),
//                                            fontSize = 10.sp
//                                        )
//                                    }
//                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RequestPage,
                                contentDescription = "Booking Requests"
                            )
                        }
                    }

                    IconButton(onClick = onCreateNewRide) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create new ride",
                            tint = Color(0xFF00A16B)
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
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF00A16B)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00A16B))
                    }
                }

                selectedTab == 0 -> {
                    // Driver trips
                    if (driverTrips.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.DirectionsCar,
                            title = "No rides as driver",
                            subtitle = "Create your first ride and start earning",
                            actionText = "Create Ride",
                            onAction = onCreateNewRide
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(driverTrips) { item ->
                                RideCard(
                                    item = item,
                                    onClick = { onRideDetails(item.trip.tripId) }
                                )
                            }
                        }
                    }
                }

                selectedTab == 1 -> {
                    // Passenger bookings
                    if (passengerBookings.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "No booked rides",
                            subtitle = "Find and book your first ride",
                            actionText = "Search Rides",
                            onAction = onNavigateBack // Navigate to home/search
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(passengerBookings) { item ->
                                RideCard(
                                    item = item,
                                    onClick = { onRideDetails(item.trip.tripId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RideCard(
    item: UserRideItem,
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
            // Header with role indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Role badge
                Surface(
                    color = if (item.role == UserRole.DRIVER) Color(0xFF00A16B) else Color(0xFF2196F3),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.role == UserRole.DRIVER)
                                Icons.Default.DirectionsCar else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.role == UserRole.DRIVER) "Driver" else "Passenger",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status badge
                val status = if (item.role == UserRole.DRIVER) {
                    item.trip.status
                } else {
                    item.booking?.status ?: "UNKNOWN"
                }
                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // From
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF00A16B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.route.startLocation,
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
                            text = item.route.endLocation,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                // Price
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$${String.format("%.2f", item.trip.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A16B)
                    )
                    if (item.role == UserRole.PASSENGER && item.booking != null && item.booking.seats > 1) {
                        Text(
                            text = "${item.booking.seats} seats",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date and time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(item.trip.departureTime)),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Additional info based on role
                when (item.role) {
                    UserRole.DRIVER -> {
                        // Show passenger count and available seats
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${item.passengerCount}/${item.trip.availableSeats + item.passengerCount} seats",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    UserRole.PASSENGER -> {
                        // Show driver name if available
                        item.driverProfile?.let { driver ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${driver.firstName} ${driver.lastName}",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Show earnings for completed driver trips
            if (item.role == UserRole.DRIVER && item.trip.status == "COMPLETED" && item.passengerCount > 0) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Earnings",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$${String.format("%.2f", item.trip.price * item.passengerCount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A16B)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "SCHEDULED" -> Color(0xFFFFF3CD) to Color(0xFF856404)
        "IN_PROGRESS" -> Color(0xFFCCE5FF) to Color(0xFF004085)
        "COMPLETED" -> Color(0xFFD4EDDA) to Color(0xFF155724)
        "CANCELLED" -> Color(0xFFF8D7DA) to Color(0xFF721C24)
        "PENDING" -> Color(0xFFFFF3CD) to Color(0xFF856404)
        "APPROVED" -> Color(0xFFD4EDDA) to Color(0xFF155724)
        "REJECTED" -> Color(0xFFF8D7DA) to Color(0xFF721C24)
        else -> Color.LightGray to Color.DarkGray
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )

            Text(
                text = subtitle,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A16B)
                )
            ) {
                Text(text = actionText)
            }
        }
    }
}