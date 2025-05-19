package ua.nure.rideshare.ui.screens.ride

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailsScreen(
    rideId: String,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit
) {
    // This would typically come from your ViewModel based on the rideId
    val ride = remember {
        RideDetails(
            id = rideId,
            driverName = "John Smith",
            driverRating = 4.8f,
            driverTrips = 142,
            driverPhotoUrl = null, // Placeholder for now
            origin = "Current Location",
            originAddress = "123 Weatherstone Rd, Santa Ana, Illinois 95405",
            destination = "Office",
            destinationAddress = "456 Thompson Dr, Dallas, Hawaii 91923",
            distance = "15 km",
            duration = "25 min",
            departureTime = "Today, 10:30 AM",
            price = "$12.50",
            vehicleName = "Mustang Shelby GT",
            vehicleColor = "Red",
            vehicleYear = "2022",
            vehiclePhotoUrl = null, // Placeholder for now
            availableSeats = 3,
            amenities = listOf("Air Conditioning", "Music", "Non-smoking")
        )
    }

    val scrollState = rememberScrollState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Trip Route Card
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
                                    text = ride.origin,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = ride.originAddress,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // Destination
                            Column {
                                Text(
                                    text = ride.destination,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = ride.destinationAddress,
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
                            value = ride.distance
                        )

                        // Duration
                        TripDetail(
                            icon = Icons.Default.Timer,
                            title = "Duration",
                            value = ride.duration
                        )

                        // Departure
                        TripDetail(
                            icon = Icons.Default.Schedule,
                            title = "Departure",
                            value = ride.departureTime
                        )
                    }
                }
            }

            // Driver and Vehicle Info Card
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
                                text = ride.driverName,
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
                                    text = "${ride.driverRating} (${ride.driverTrips} trips)",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp)
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

                    Divider(
                        color = Color.LightGray,
                        thickness = 1.dp
                    )

                    // Vehicle info
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
                            Text(
                                text = "🚗",
                                fontSize = 24.sp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = ride.vehicleName,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "${ride.vehicleColor}, ${ride.vehicleYear}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Ride Details Card
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
                        value = "${ride.availableSeats}"
                    )

                    // Price
                    DetailRow(
                        icon = Icons.Default.AttachMoney,
                        title = "Price",
                        value = ride.price
                    )

                    // Amenities
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Amenities",
                            fontWeight = FontWeight.Medium
                        )

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ride.amenities.forEach { amenity ->
                                Chip(
                                    amenity = amenity,
                                    onClick = { }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom padding for button
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Booking button (floating at bottom)
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = onBookClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A16B)
            )
        ) {
            Text(
                text = "Book this ride - ${ride.price}",
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

// Data class for ride details
data class RideDetails(
    val id: String,
    val driverName: String,
    val driverRating: Float,
    val driverTrips: Int,
    val driverPhotoUrl: String?,
    val origin: String,
    val originAddress: String,
    val destination: String,
    val destinationAddress: String,
    val distance: String,
    val duration: String,
    val departureTime: String,
    val price: String,
    val vehicleName: String,
    val vehicleColor: String,
    val vehicleYear: String,
    val vehiclePhotoUrl: String?,
    val availableSeats: Int,
    val amenities: List<String>
)