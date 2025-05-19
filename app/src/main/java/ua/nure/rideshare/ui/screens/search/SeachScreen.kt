package ua.nure.rideshare.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.nure.rideshare.ui.viewmodels.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    locationViewModel: LocationViewModel,
    origin: String,
    destination: String,
    onNavigateToRideDetails: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(true) }
    var selectedAddress by remember { mutableStateOf("") }

    // Show the modal bottom sheet
    LaunchedEffect(key1 = Unit) {
        sheetState.show()
    }

    // Handle back button
    BackHandler(enabled = sheetState.isVisible) {
        scope.launch {
            isVisible = false
            sheetState.hide()
            onBackClick()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    isVisible = false
                    onBackClick()
                }
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color.White,
            contentColor = Color.Black,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Divider(
                        color = Color.LightGray,
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header with title and close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Select address",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = {
                            scope.launch {
                                isVisible = false
                                sheetState.hide()
                                onBackClick()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Current location option
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { selectedAddress = "Current location" },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.White
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp
//                        color = Color(0xFF00A16B)

                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Current Location",
                            tint = Color(0xFF00A16B),
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Text(
                            text = "Current location",
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Other location option
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { selectedAddress = "United state" },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.White
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp
//                        color = Color.LightGray
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Gray,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Text(
                            text = "United state",
                            fontSize = 16.sp
                        )
                    }
                }

                // Recent places section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recent places",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Recent place item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAddress = "Office" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.Black,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Office",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "123 Weatherstone Rd, Santa Ana, Illinois 95405",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }

                        Text(
                            text = "2.7km",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (selectedAddress.isNotEmpty()) {
                                onNavigateToRideDetails("sample-ride-id")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Ride",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { /* Handle Go action */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Go",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Composable for handling back press
@Composable
private fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    // In a real app, you would use androidx.activity.compose.BackHandler
    // but we're implementing this stub for the example
    LaunchedEffect(enabled) {
        // Would hook into the system back button in a real implementation
    }
}