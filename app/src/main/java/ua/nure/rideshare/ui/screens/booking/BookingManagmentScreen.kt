package ua.nure.rideshare.ui.screens.booking

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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import ua.nure.rideshare.ui.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingManagementScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTripDetails: (String) -> Unit,
    bookingViewModel: BookingRequestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // UI state
    var selectedTab by remember { mutableStateOf(0) }
    var showRejectDialog by remember { mutableStateOf<BookingRequestData?>(null) }

    // ViewModel states
    val incomingRequests by bookingViewModel.incomingBookingRequests.collectAsState()
    val userRequests by bookingViewModel.userBookingRequests.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val errorMessage by bookingViewModel.errorMessage.collectAsState()

    // Initialize with user ID
    LaunchedEffect(userId) {
        bookingViewModel.setCurrentUserId(userId)
    }

    // Handle booking events
    LaunchedEffect(Unit) {
        bookingViewModel.bookingEvent.collectLatest { event ->
            when (event) {
                is BookingEvent.BookingApproved -> {
                    Toast.makeText(context, "Booking request approved", Toast.LENGTH_SHORT).show()
                }
                is BookingEvent.BookingRejected -> {
                    Toast.makeText(context, "Booking request rejected", Toast.LENGTH_SHORT).show()
                }
                is BookingEvent.BookingCancelled -> {
                    Toast.makeText(context, "Booking request cancelled", Toast.LENGTH_SHORT).show()
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
                title = { Text("Booking Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { bookingViewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
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
                contentColor = Color(0xFF00A16B),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00A16B)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Incoming",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal
                            )
                            if (incomingRequests.isNotEmpty()) {
                                Text(
                                    text = "(${incomingRequests.size})",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00A16B)
                                )
                            }
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "My Requests",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal
                            )
                            if (userRequests.isNotEmpty()) {
                                Text(
                                    text = "(${userRequests.size})",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00A16B)
                                )
                            }
                        }
                    }
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF00A16B)
                        )
                    }

                    selectedTab == 0 -> {
                        // Incoming requests (for drivers)
                        if (incomingRequests.isEmpty()) {
                            EmptyIncomingRequestsState()
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(incomingRequests) { request ->
                                    IncomingBookingRequestCard(
                                        request = request,
                                        onAccept = {
                                            coroutineScope.launch {
                                                bookingViewModel.acceptBookingRequest(request.booking.bookingId)
                                            }
                                        },
                                        onReject = {
                                            showRejectDialog = request
                                        },
                                        onViewTrip = {
                                            onNavigateToTripDetails(request.trip.tripId)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    selectedTab == 1 -> {
                        // User's requests (for passengers)
                        if (userRequests.isEmpty()) {
                            EmptyUserRequestsState()
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(userRequests) { request ->
                                    UserBookingRequestCard(
                                        request = request,
                                        onCancel = {
                                            coroutineScope.launch {
                                                bookingViewModel.cancelBookingRequest(request.booking.bookingId)
                                            }
                                        },
                                        onViewTrip = {
                                            onNavigateToTripDetails(request.trip.tripId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reject dialog
    showRejectDialog?.let { request ->
        RejectBookingDialog(
            passengerName = "${request.passengerProfile.firstName} ${request.passengerProfile.lastName}",
            onConfirm = { reason ->
                coroutineScope.launch {
                    bookingViewModel.rejectBookingRequest(request.booking.bookingId, reason)
                    showRejectDialog = null
                }
            },
            onDismiss = { showRejectDialog = null }
        )
    }
}

@Composable
fun IncomingBookingRequestCard(
    request: BookingRequestData,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onViewTrip: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewTrip),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (request.booking.status == "PENDING") Color(0xFFFFF8E1) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with passenger info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Passenger avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A16B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.passengerProfile.firstName.first().toString() +
                                request.passengerProfile.lastName.first().toString(),
                        fontSize = 16.sp,
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
                        text = "${request.passengerProfile.firstName} ${request.passengerProfile.lastName}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
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
                            text = "${request.passengerProfile.rating} rating",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                StatusBadge(status = request.booking.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF00A16B),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = request.route.startLocation,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = request.route.endLocation,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Booking details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Seats Requested",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${request.booking.seats}",
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Departure",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = dateFormatter.format(Date(request.trip.departureTime)),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            // Pickup/Dropoff locations if specified
            if (!request.booking.pickupLocation.isNullOrBlank() || !request.booking.dropoffLocation.isNullOrBlank()) {
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                request.booking.pickupLocation?.let { pickup ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Pickup: $pickup",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                request.booking.dropoffLocation?.let { dropoff ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Dropoff: $dropoff",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Action buttons (only show for pending requests)
            if (request.booking.status == "PENDING") {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reject",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A16B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Composable
fun UserBookingRequestCard(
    request: BookingRequestData,
    onCancel: () -> Unit,
    onViewTrip: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewTrip),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking Request",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                StatusBadge(status = request.booking.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF00A16B),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = request.route.startLocation,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = request.route.endLocation,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Booking details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Seats",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${request.booking.seats}",
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Total Price",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", request.trip.price * request.booking.seats)}",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00A16B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Departure",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = dateFormatter.format(Date(request.trip.departureTime)),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            // Request date
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Requested on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(request.booking.createdAt))}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            // Cancel button (only show for pending or approved requests)
            if (request.booking.status == "PENDING" || request.booking.status == "APPROVED") {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel Request")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor, text) = when (status) {
        "PENDING" -> Triple(Color(0x1AFF9800), Color(0xFFFF9800), "Pending")
        "APPROVED" -> Triple(Color(0x1A4CAF50), Color(0xFF4CAF50), "Approved")
        "REJECTED" -> Triple(Color(0x1AF44336), Color(0xFFF44336), "Rejected")
        "CANCELLED" -> Triple(Color(0x1A757575), Color(0xFF757575), "Cancelled")
        "COMPLETED" -> Triple(Color(0x1A2196F3), Color(0xFF2196F3), "Completed")
        else -> Triple(Color.LightGray, Color.Gray, status)
    }

    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyIncomingRequestsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )

        Text(
            text = "No Incoming Requests",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Booking requests for your trips will appear here",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun EmptyUserRequestsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )

        Text(
            text = "No Booking Requests",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Your booking requests will appear here",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun RejectBookingDialog(
    passengerName: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Booking Request") },
        text = {
            Column {
                Text("Are you sure you want to reject the booking request from $passengerName?")

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("Let them know why...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00A16B),
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = Color(0xFF00A16B)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.takeIf { it.isNotBlank() }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}