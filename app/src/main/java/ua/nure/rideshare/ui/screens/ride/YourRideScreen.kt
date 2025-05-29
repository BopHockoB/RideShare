package ua.nure.rideshare.ui.screens.ride

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.ui.viewmodels.RideViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourRideScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onCreateNewRide: () -> Unit,
    onRideDetails: (String) -> Unit,
    viewModel: RideViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State from ViewModel
    val userTrips by viewModel.userTrips.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val tripCreationState by viewModel.tripCreationState.collectAsState()

    // UI state
    var selectedTab by remember { mutableStateOf(0) }
    var showCancelDialog by remember { mutableStateOf<Trip?>(null) }

    // Initialize ViewModel with user ID
    LaunchedEffect(userId) {
        viewModel.setCurrentUserId(userId)
        viewModel.loadUserTrips(userId)
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    // Filter trips based on selected tab
    val filteredTrips = remember(userTrips, selectedTab) {
        when (selectedTab) {
            0 -> userTrips.filter { it.status == "SCHEDULED" } // Upcoming
            1 -> userTrips.filter { it.status == "IN_PROGRESS" } // Active
            2 -> userTrips.filter { it.status == "COMPLETED" } // Completed
            3 -> userTrips.filter { it.status == "CANCELLED" } // Cancelled
            else -> userTrips
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
                    IconButton(onClick = onCreateNewRide) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Ride"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewRide,
                containerColor = Color(0xFF00A16B),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Ride"
                )
            }
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
                val tabs = listOf("Upcoming", "Active", "Completed", "Cancelled")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (filteredTrips.isEmpty()) {
                    // Empty state
                    EmptyRideState(
                        selectedTab = selectedTab,
                        onCreateRide = onCreateNewRide
                    )
                } else {
                    // Ride list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTrips) { trip ->
                            RideCard(
                                trip = trip,
                                onRideClick = { onRideDetails(trip.tripId) },
                                onCancelRide = { showCancelDialog = trip },
                                onStartRide = {
                                    coroutineScope.launch {
                                        try {
                                            viewModel.updateTripStatus(trip.tripId, "IN_PROGRESS")
                                            Toast.makeText(context, "Ride started", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error starting ride", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onCompleteRide = {
                                    coroutineScope.launch {
                                        try {
                                            viewModel.updateTripStatus(trip.tripId, "COMPLETED")
                                            Toast.makeText(context, "Ride completed", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error completing ride", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }

                        // Add space at the bottom for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Cancel confirmation dialog
    showCancelDialog?.let { trip ->
        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("Cancel Ride") },
            text = { Text("Are you sure you want to cancel this ride? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                viewModel.cancelTrip(trip.tripId)
                                Toast.makeText(context, "Ride cancelled", Toast.LENGTH_SHORT).show()
                                showCancelDialog = null
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error cancelling ride", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Cancel Ride")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCancelDialog = null }
                ) {
                    Text("Keep Ride")
                }
            }
        )
    }
}

@Composable
fun EmptyRideState(
    selectedTab: Int,
    onCreateRide: () -> Unit
) {
    val (title, subtitle, icon) = when (selectedTab) {
        0 -> Triple("No upcoming rides", "Create a ride to start offering rides to passengers", Icons.Default.Schedule)
        1 -> Triple("No active rides", "Your active rides will appear here", Icons.Default.DirectionsCar)
        2 -> Triple("No completed rides", "Your completed rides will appear here", Icons.Default.CheckCircle)
        3 -> Triple("No cancelled rides", "Your cancelled rides will appear here", Icons.Default.Cancel)
        else -> Triple("No rides", "Start by creating your first ride", Icons.Default.DirectionsCar)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        if (selectedTab == 0) { // Only show create button for upcoming rides
            Button(
                onClick = onCreateRide,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Create a Ride")
            }
        }
    }
}

@Composable
fun RideCard(
    trip: Trip,
    onRideClick: () -> Unit,
    onCancelRide: () -> Unit,
    onStartRide: () -> Unit,
    onCompleteRide: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRideClick),
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
                // Status badge
                StatusBadge(status = trip.status)

                // Price
                Text(
                    text = "$${String.format("%.2f", trip.price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A16B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route information (simplified since we don't have route details loaded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route visualization
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

                // Route details placeholder (you would need to join with Route table to get actual data)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Route Details",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Destination",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Date and time
                Column {
                    Text(
                        text = "Departure",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${dateFormatter.format(Date(trip.departureTime))} at ${timeFormatter.format(Date(trip.departureTime))}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Available seats
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Available Seats",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${trip.availableSeats}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Notes if available
            if (!trip.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${trip.notes}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Action buttons based on status
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (trip.status) {
                    "SCHEDULED" -> {
                        OutlinedButton(
                            onClick = onCancelRide,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onStartRide,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A16B)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Start Ride")
                        }
                    }
                    "IN_PROGRESS" -> {
                        Button(
                            onClick = onCompleteRide,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A16B)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Complete Ride")
                        }
                    }
                    "COMPLETED" -> {
                        Button(
                            onClick = { /* View details */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View Details")
                        }
                    }
                    "CANCELLED" -> {
                        // No actions for cancelled rides
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor, text) = when (status) {
        "SCHEDULED" -> Triple(Color(0x1A2196F3), Color(0xFF2196F3), "Scheduled")
        "IN_PROGRESS" -> Triple(Color(0x1AFF9800), Color(0xFFFF9800), "In Progress")
        "COMPLETED" -> Triple(Color(0x1A4CAF50), Color(0xFF4CAF50), "Completed")
        "CANCELLED" -> Triple(Color(0x1AF44336), Color(0xFFF44336), "Cancelled")
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