@file:OptIn(ExperimentalMaterial3Api::class)

package ua.nure.rideshare.ui.screens.ride

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideCreationScreen(
    locationViewModel: LocationViewModel,
    onBackClick: () -> Unit,
    onConfirmTrip: (startLocation: String, endLocation: String, date: String, time: String, vehicle: String) -> Unit
) {
    var startLocation by remember { mutableStateOf("Current location") }
    var startAddress by remember { mutableStateOf("123 Weatherstone Rd, Santa Ana, Illinois 95405") }
    var endLocation by remember { mutableStateOf("Office") }
    var endAddress by remember { mutableStateOf("456 Thompson Dr, Dallas, Hawaii 91923") }
    var selectedVehicle by remember { mutableStateOf("Mustang Shelby GT") }
    var vehicleRating by remember { mutableStateOf(4.9f) }
    var vehicleTripCount by remember { mutableStateOf(176) }

    // Date and time selections
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Date and time formatters
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // App Bar
        SmallTopAppBar(
            title = { Text(text = "Request for rent") },
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
            // Start Location
            LocationItem(
                locationName = startLocation,
                address = startAddress,
                iconTint = Color.Red,
                modifier = Modifier.padding(bottom = 12.dp),
                onClick = { /* Open location selector */ }
            )

            // End Location
            LocationItem(
                locationName = endLocation,
                address = endAddress,
                iconTint = Color(0xFF00A16B),
                distance = "1.1km",
                modifier = Modifier.padding(bottom = 16.dp),
                onClick = { /* Open location selector */ }
            )

            // Vehicle Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF00A16B),
                        shape = RoundedCornerShape(8.dp)
                    ),
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
                        Text(
                            text = selectedVehicle,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00A16B)
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
                                text = "$vehicleRating ($vehicleTripCount)",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Car image placeholder
                    Box(
                        modifier = Modifier
                            .size(80.dp, 40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.LightGray)
                    ) {
                        // Replace with actual car image
                        // This is a placeholder
                        Text(
                            text = "🚗",
                            fontSize = 20.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Date Selection
            OutlinedTextField(
                value = selectedDate?.format(dateFormatter) ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select date"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray
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
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Select time"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { showTimePicker = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Confirm Trip Button
            Button(
                onClick = {
                    // Get formatted date and time or empty string if null
                    val dateStr = selectedDate?.format(dateFormatter) ?: ""
                    val timeStr = selectedTime?.format(timeFormatter) ?: ""

                    onConfirmTrip(startLocation, endLocation, dateStr, timeStr, selectedVehicle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A16B)
                ),
                enabled = selectedDate != null && selectedTime != null
            ) {
                Text(
                    text = "Confirm Trip",
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