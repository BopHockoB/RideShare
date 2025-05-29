@file:OptIn(ExperimentalMaterial3Api::class)

package ua.nure.rideshare.ui.screens.ride

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.ui.screens.location.LocationSelectionScreen
import ua.nure.rideshare.ui.viewmodels.CarViewModel
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import ua.nure.rideshare.ui.viewmodels.RideViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RideCreationScreen(
    userId: String,
    locationViewModel: LocationViewModel,
    carViewModel: CarViewModel = hiltViewModel(),
    rideViewModel: RideViewModel = hiltViewModel(),
    selectedCarId: String? = null,
    onBackClick: () -> Unit,
    onConfirmTrip: (startLocation: String, endLocation: String, date: String, time: String, vehicle: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State for starting and ending locations
    var startLocation by remember { mutableStateOf("Current location") }
    var startAddress by remember { mutableStateOf("123 Weatherstone Rd, Santa Ana, Illinois 95405") }
    var startLatitude by remember { mutableStateOf(40.7128) } // Default value (New York)
    var startLongitude by remember { mutableStateOf(-74.0060) } // Default value

    var endLocation by remember { mutableStateOf("Office") }
    var endAddress by remember { mutableStateOf("456 Thompson Dr, Dallas, Hawaii 91923") }
    var endLatitude by remember { mutableStateOf(40.7328) } // Default value slightly different
    var endLongitude by remember { mutableStateOf(-73.9860) } // Default value slightly different

    // Car selection state
    var selectedCar by remember { mutableStateOf<Car?>(null) }
    var showCarSelector by remember { mutableStateOf(false) }

    // Price and seats
    var price by remember { mutableStateOf("15.00") }
    var availableSeats by remember { mutableStateOf(4) }

    // Date and time selections
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Additional notes
    var notes by remember { mutableStateOf("") }

    // Date and time formatters
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    // Form validation
    var isFormValid by remember { mutableStateOf(false) }

    // Get user location if available
    val userLocation by locationViewModel.userLocation.collectAsState(initial = null)

    // Get user's cars
    val userCars by carViewModel.userCars.collectAsState(initial = emptyList())
    val isLoadingCars by carViewModel.isLoading.collectAsState(initial = true)

    // Load user's active cars
    LaunchedEffect(Unit) {
        val userId = rideViewModel.getCurrentUserId()
        Log.d("RIDE_CREATION", "=== CAR LOADING DEBUG ===")
        Log.d("RIDE_CREATION", "Current userId: $userId")
        Log.d("RIDE_CREATION", "selectedCarId parameter: $selectedCarId")

        if (userId != null) {
            Log.d("RIDE_CREATION", "Loading ALL cars for user: $userId")
            carViewModel.loadUserCars(userId) // Load ALL cars first

            // If we have a specific car ID, also try to load that specific car
            selectedCarId?.let { carId ->
                Log.d("RIDE_CREATION", "Also loading specific car: $carId")
                carViewModel.loadCarById(carId)
            }
        } else {
            Log.e("RIDE_CREATION", "UserId is null! Cannot load cars")
        }
    }

    // Use user's location if available
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            startLatitude = location.latitude
            startLongitude = location.longitude
        }
    }

    LaunchedEffect(userCars, selectedCarId, isLoadingCars) {
        Log.d("RIDE_CREATION", "=== CAR SELECTION DEBUG ===")
        Log.d("RIDE_CREATION", "Total cars loaded: ${userCars.size}")
        Log.d("RIDE_CREATION", "Is loading: $isLoadingCars")
        Log.d("RIDE_CREATION", "Selected car ID: $selectedCarId")

        // Log all available cars with detailed info
        userCars.forEachIndexed { index, car ->
            Log.d("RIDE_CREATION", "Car $index: ID=${car.carId}, ${car.make} ${car.model}, Active=${car.isActive}, Owner=${car.ownerId}")
        }

        if (userCars.isNotEmpty()) {
            val foundCar = if (selectedCarId != null) {
                userCars.find { it.carId == selectedCarId }.also { found ->
                    if (found != null) {
                        Log.d("RIDE_CREATION", "✅ Found matching car: ${found.make} ${found.model}")
                    } else {
                        Log.w("RIDE_CREATION", "❌ No car found with ID: $selectedCarId")
                        Log.w("RIDE_CREATION", "Available car IDs: ${userCars.map { it.carId }}")
                    }
                }
            } else {
                userCars.firstOrNull().also {
                    Log.d("RIDE_CREATION", "No specific carId, using first available: ${it?.make} ${it?.model}")
                }
            }

            selectedCar = foundCar

            // Update available seats based on selected car
            selectedCar?.let { car ->
                availableSeats = minOf(availableSeats, car.seats)
                Log.d("RIDE_CREATION", "✅ Selected car: ${car.make} ${car.model}, Available seats: $availableSeats")
            } ?: run {
                Log.w("RIDE_CREATION", "❌ No car selected!")
            }
        } else if (!isLoadingCars) {
            Log.w("RIDE_CREATION", "❌ No cars loaded and loading finished!")

            // If we have a selectedCarId but no cars loaded, try loading that specific car
            selectedCarId?.let { carId ->
                Log.d("RIDE_CREATION", "Attempting to load the specific car: $carId")
                carViewModel.addSpecificCarToList(carId)
            }
        }
    }

    LaunchedEffect(Unit) {
        carViewModel.errorMessage.collect { error ->
            error?.let {
                Log.e("RIDE_CREATION", "CarViewModel error: $it")
            }
        }
    }

    // Validate form
    LaunchedEffect(startLocation, endLocation, selectedDate, selectedTime, selectedCar, price) {
        isFormValid = startLocation.isNotBlank() &&
                endLocation.isNotBlank() &&
                selectedDate != null &&
                selectedTime != null &&
                selectedCar != null &&
                price.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
    ) {
        // App Bar
        SmallTopAppBar(
            title = { Text(text = "Create New Ride") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Location selection state
            var showStartLocationSelector by remember { mutableStateOf(false) }
            var showEndLocationSelector by remember { mutableStateOf(false) }

            // Start Location
            LocationItem(
                locationName = startLocation,
                address = startAddress,
                iconTint = Color.Red,
                modifier = Modifier.padding(bottom = 12.dp),
                onClick = { showStartLocationSelector = true }
            )

            // End Location
            LocationItem(
                locationName = endLocation,
                address = endAddress,
                iconTint = Color(0xFF00A16B),
                distance = calculateDistance(startLatitude, startLongitude, endLatitude, endLongitude),
                modifier = Modifier.padding(bottom = 16.dp),
                onClick = { showEndLocationSelector = true }
            )

            // Start location selection dialog
            if (showStartLocationSelector) {
                LocationSelectionScreen(
                    initialQuery = startLocation,
                    onLocationSelected = { locationData ->
                        startLocation = locationData.name
                        startAddress = locationData.address
                        startLatitude = locationData.latLng.latitude
                        startLongitude = locationData.latLng.longitude
                        showStartLocationSelector = false
                    },
                    onDismiss = { showStartLocationSelector = false },
                    locationViewModel = locationViewModel
                )
            }

            // End location selection dialog
            if (showEndLocationSelector) {
                LocationSelectionScreen(
                    initialQuery = endLocation,
                    onLocationSelected = { locationData ->
                        endLocation = locationData.name
                        endAddress = locationData.address
                        endLatitude = locationData.latLng.latitude
                        endLongitude = locationData.latLng.longitude
                        showEndLocationSelector = false
                    },
                    onDismiss = { showEndLocationSelector = false },
                    locationViewModel = locationViewModel
                )
            }

            // Vehicle Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF00A16B),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { showCarSelector = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE6F7F0)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (selectedCar != null) {
                            Text(
                                text = "${selectedCar!!.make} ${selectedCar!!.model}",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF00A16B)
                            )

                            Text(
                                text = "${selectedCar!!.year} • ${selectedCar!!.color} • ${selectedCar!!.seats} seats",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = if (isLoadingCars) "Loading cars..." else if (userCars.isEmpty()) "No cars available" else "Select a car",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF00A16B)
                            )
                        }
                    }

                    // Car image or icon
                    Box(
                        modifier = Modifier
                            .size(80.dp, 40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        // Replace with actual car image
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car",
                            tint = Color(0xFF00A16B)
                        )
                    }
                }
            }

            // Price field
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price ($)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = "Price",
                        tint = Color(0xFF00A16B)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Available seats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Seats:",
                    modifier = Modifier.weight(1f)
                )

                // Decrease button
                IconButton(
                    onClick = { if (availableSeats > 1) availableSeats-- },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape),
                    enabled = availableSeats > 1 // Disable if at minimum
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease seats",
                        tint = if (availableSeats > 1) Color(0xFF00A16B) else Color.Gray
                    )
                }

                Text(
                    text = availableSeats.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                // Increase button - Fixed logic
                val maxSeats = selectedCar?.seats ?: 4 // Default to 4 if no car selected
                IconButton(
                    onClick = {
                        if (availableSeats < maxSeats) {
                            availableSeats++
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape),
                    enabled = availableSeats < maxSeats // Disable if at maximum
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase seats",
                        tint = if (availableSeats < maxSeats) Color(0xFF00A16B) else Color.Gray
                    )
                }
            }

            // Date Selection
            OutlinedTextField(
                value = selectedDate?.format(dateFormatter) ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("Date") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Calendar",
                        tint = Color(0xFF00A16B)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select date"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { showDatePicker = true }
            )

            // Time Selection
            OutlinedTextField(
                value = selectedTime?.format(timeFormatter) ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("Time") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Time",
                        tint = Color(0xFF00A16B)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select time"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { showTimePicker = true }
            )

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Note,
                        contentDescription = "Notes",
                        tint = Color(0xFF00A16B)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF00A16B)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Trip Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            selectedCar?.let { car ->
                                // Convert price to Double
                                val priceValue = price.toDoubleOrNull() ?: 0.0

                                // Convert selected date/time to milliseconds epoch time
                                val localDateTime = selectedDate?.atTime(selectedTime ?: LocalTime.NOON)
                                val departureTimeMillis = localDateTime?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000) ?: System.currentTimeMillis()

                                // Create trip using the provided userId
                                val tripId = rideViewModel.createTrip( // You might need to add this method
                                    userId = userId, // Use the provided userId
                                    startLocationName = startLocation,
                                    startAddress = startAddress,
                                    startLatitude = startLatitude,
                                    startLongitude = startLongitude,
                                    endLocationName = endLocation,
                                    endAddress = endAddress,
                                    endLatitude = endLatitude,
                                    endLongitude = endLongitude,
                                    departureTime = departureTimeMillis,
                                    price = priceValue,
                                    availableSeats = availableSeats,
                                    carId = car.carId,
                                    notes = notes.ifBlank { null }
                                )

                                Toast.makeText(context, "Trip created successfully", Toast.LENGTH_SHORT).show()

                                // Get formatted date and time or empty string if null
                                val dateStr = selectedDate?.format(dateFormatter) ?: ""
                                val timeStr = selectedTime?.format(timeFormatter) ?: ""

                                onConfirmTrip(startLocation, endLocation, dateStr, timeStr, "${car.make} ${car.model}")
                            } ?: run {
                                Toast.makeText(context, "Please select a car first", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error creating trip: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                // ... rest of button properties
            ) {
                Text(
                    text = "Create Trip",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
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

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showTimePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    // Car Selector Dialog
    if (showCarSelector) {
        CarSelectorDialog(
            cars = userCars,
            selectedCar = selectedCar,
            onCarSelected = { car ->
                selectedCar = car
                availableSeats = car.seats
                showCarSelector = false
            },
            onDismiss = { showCarSelector = false }
        )
    }
}

@Composable
fun CarSelectorDialog(
    cars: List<Car>,
    selectedCar: Car?,
    onCarSelected: (Car) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select a Car",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (cars.isEmpty()) {
                    Text(
                        text = "No cars available. Please add a car first.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        cars.forEach { car ->
                            val isSelected = selectedCar?.carId == car.carId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { onCarSelected(car) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onCarSelected(car) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF00A16B)
                                    )
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "${car.make} ${car.model}",
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "${car.year} • ${car.color} • ${car.seats} seats",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )

                                    Text(
                                        text = "License: ${car.licensePlate}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF00A16B)
                                    )
                                }
                            }

                            if (car != cars.last()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    if (cars.isNotEmpty()) {
                        Button(
                            onClick = {
                                selectedCar?.let { onCarSelected(it) }
                                    ?: run { onCarSelected(cars.first()) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A16B)
                            )
                        ) {
                            Text("Select")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    locationName: String,
    address: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    distance: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
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

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = locationName,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = address,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        if (distance != null) {
            Text(
                text = distance,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties()
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp),
            propagateMinConstraints = true
        ) {
            Column {
                // Content
                content()

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Dismiss button
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        dismissButton()
                    }

                    // Confirm button
                    confirmButton()
                }
            }
        }
    }
}

// Helper function to calculate distance between coordinates
private fun calculateDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): String {
    val radiusEarth = 6371.0 // Earth's radius in km

    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    val distance = radiusEarth * c

    return if (distance < 1) {
        "${(distance * 1000).toInt()} m"
    } else {
        String.format("%.1f km", distance)
    }
}
