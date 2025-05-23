package ua.nure.rideshare.ui.screens.car

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
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
import ua.nure.rideshare.ui.viewmodels.CarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarCreationScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onCarCreated: (String) -> Unit,
    viewModel: CarViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Form state
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("4") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    // Available amenities
    val availableAmenities = listOf(
        "Air Conditioning",
        "Heating",
        "Music",
        "USB Charger",
        "Non-smoking",
        "Pet Friendly",
        "Wi-Fi",
        "Water Provided",
        "Luggage Space"
    )

    // Validate form on input changes
    LaunchedEffect(make, model, year, color, licensePlate, seats) {
        isFormValid = make.isNotBlank() &&
                model.isNotBlank() &&
                year.isNotBlank() &&
                color.isNotBlank() &&
                licensePlate.isNotBlank() &&
                seats.isNotBlank()
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Add New Car") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Photo upload section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable {
                        // In a real app, we would launch a photo picker here
                        // For now, we'll just simulate it with a dummy URL
                        photoUrl = "https://example.com/car.jpg"
                        Toast.makeText(context, "Photo upload simulated", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    // In a real app, we would use AsyncImage to load the photo
                    // For now, we'll just show a success message
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Photo Uploaded",
                            tint = Color(0xFF00A16B),
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = "Photo uploaded",
                            color = Color(0xFF00A16B),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Upload Photo",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = "Upload car photo",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Car information fields
            Text(
                text = "Car Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Make field
            OutlinedTextField(
                value = make,
                onValueChange = { make = it },
                label = { Text("Make*") },
                placeholder = { Text("e.g. Toyota") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF00A16B),
                    cursorColor = Color(0xFF00A16B)
                )
            )

            // Model field
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model*") },
                placeholder = { Text("e.g. Corolla") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A16B),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF00A16B),
                    cursorColor = Color(0xFF00A16B)
                )
            )

            // Year and Color in a row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year field
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year*") },
                    placeholder = { Text("e.g. 2022") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00A16B),
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color(0xFF00A16B),
                        cursorColor = Color(0xFF00A16B)
                    )
                )

                // Color field
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color*") },
                    placeholder = { Text("e.g. Blue") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00A16B),
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color(0xFF00A16B),
                        cursorColor = Color(0xFF00A16B)
                    )
                )
            }

            // License plate and seats in a row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // License plate field
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("License Plate*") },
                    placeholder = { Text("e.g. ABC123") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00A16B),
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color(0xFF00A16B),
                        cursorColor = Color(0xFF00A16B)
                    )
                )

                // Seats field
                OutlinedTextField(
                    value = seats,
                    onValueChange = { seats = it },
                    label = { Text("Seats*") },
                    placeholder = { Text("e.g. 4") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00A16B),
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color(0xFF00A16B),
                        cursorColor = Color(0xFF00A16B)
                    )
                )
            }

            // Amenities
            Text(
                text = "Amenities",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            AmenitiesSection(
                availableAmenities = availableAmenities,
                selectedAmenities = selectedAmenities,
                onAmenitySelected = { amenity, selected ->
                    selectedAmenities = if (selected) {
                        selectedAmenities + amenity
                    } else {
                        selectedAmenities - amenity
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Add Car Button
            Button(
                onClick = {
                    // Create the car
                    coroutineScope.launch {
                        try {
                            val yearInt = year.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
                            val seatsInt = seats.toIntOrNull() ?: throw IllegalArgumentException("Invalid seats")

                            val amenitiesString = if (selectedAmenities.isEmpty()) null else selectedAmenities.joinToString(",")

                            val carId = viewModel.createCar(
                                ownerId = userId,
                                make = make,
                                model = model,
                                year = yearInt,
                                color = color,
                                licensePlate = licensePlate,
                                photoUrl = photoUrl,
                                seats = seatsInt,
                                amenities = amenitiesString,
                                isActive = true
                            )

                            Toast.makeText(context, "Car added successfully", Toast.LENGTH_SHORT).show()
                            onCarCreated(carId)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A16B),
                    disabledContainerColor = Color(0xFF00A16B).copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Add Car",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Add space at the bottom for better UX
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AmenitiesSection(
    availableAmenities: List<String>,
    selectedAmenities: Set<String>,
    onAmenitySelected: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (amenity in availableAmenities) {
            val isSelected = selectedAmenities.contains(amenity)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAmenitySelected(amenity, !isSelected) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onAmenitySelected(amenity, it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00A16B),
                        uncheckedColor = Color.Gray
                    )
                )

                Text(
                    text = amenity,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
