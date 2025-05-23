package ua.nure.rideshare.ui.screens.car

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.ui.viewmodels.CarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCarsScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onAddNewCar: () -> Unit,
    onSelectCar: (String) -> Unit,
    viewModel: CarViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Cars state
    val cars by viewModel.userCars.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = true)

    // Load cars on start
    LaunchedEffect(userId) {
        viewModel.loadUserCars(userId)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Your Cars") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewCar,
                containerColor = Color(0xFF00A16B),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Car"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF00A16B)
                )
            } else if (cars.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.LightGray
                    )

                    Text(
                        text = "No cars added yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Text(
                        text = "Add a car to start offering rides",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    Button(
                        onClick = onAddNewCar,
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
                        Text("Add a Car")
                    }
                }
            } else {
                // Car list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cars) { car ->
                        CarItem(
                            car = car,
                            onCarSelected = { onSelectCar(car.carId) },
                            onToggleActive = {
                                coroutineScope.launch {
                                    viewModel.updateCarActiveStatus(car.carId, !car.isActive)
                                    Toast.makeText(
                                        context,
                                        if (car.isActive) "Car deactivated" else "Car activated",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onDeleteCar = {
                                coroutineScope.launch {
                                    viewModel.deleteCar(car)
                                    Toast.makeText(
                                        context,
                                        "Car deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    // Add space after the list for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CarItem(
    car: Car,
    onCarSelected: () -> Unit,
    onToggleActive: () -> Unit,
    onDeleteCar: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCarSelected),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Car info section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Car icon or image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // If car has photo, show it, otherwise show icon
                    if (car.photoUrl != null) {
                        // Would use AsyncImage here with Coil
                        // For now using simple car emoji
                        Text(
                            text = "🚗",
                            fontSize = 24.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Car details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "${car.make} ${car.model}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "${car.year} • ${car.color} • ${car.seats} seats",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "License: ${car.licensePlate}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Status indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (car.isActive) Color(0xFF00A16B) else Color.Gray)
                        .border(1.dp, Color.White, CircleShape)
                )
            }

            // Amenities
            if (!car.amenities.isNullOrBlank()) {
                val amenitiesList = car.amenities.split(",")
                if (amenitiesList.isNotEmpty()) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.LightGray
                    )

                    Text(
                        text = "Amenities",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Show first 3 amenities or less
                        val displayedAmenities = if (amenitiesList.size > 3) {
                            amenitiesList.take(3) + "..."
                        } else {
                            amenitiesList
                        }

                        displayedAmenities.forEach { amenity ->
                            Surface(
                                modifier = Modifier,
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x1A00A16B) // Light green with 10% opacity
                            ) {
                                Text(
                                    text = amenity,
                                    color = Color(0xFF00A16B),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // Delete button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Car",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle active button
                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (car.isActive) Color.Gray else Color(0xFF00A16B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (car.isActive) Icons.Default.DoNotDisturb else Icons.Default.CheckCircle,
                        contentDescription = if (car.isActive) "Deactivate" else "Activate",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (car.isActive) "Deactivate" else "Activate")
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Car") },
            text = { Text("Are you sure you want to delete this car? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCar()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}