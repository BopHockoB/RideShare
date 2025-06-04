package ua.nure.rideshare.ui.screens.profile

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import ua.nure.rideshare.ui.viewmodels.ProfileEvent
import ua.nure.rideshare.ui.viewmodels.ProfileState
import ua.nure.rideshare.ui.viewmodels.ProfileViewModel


@Composable
fun ProfileMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF00A16B)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00A16B),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF00A16B),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF00A16B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateToHome: () -> Unit,
    onNavigateToUserCars: () -> Unit,
    onNavigateToUserRides: () -> Unit,
//    onNavigateToBookingManagement: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Collect state from view model
    val profileState by viewModel.profileState.collectAsState()
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    // Collect one-time events
    LaunchedEffect(Unit) {
        viewModel.profileEvent.collectLatest { event ->
            when (event) {
                is ProfileEvent.Logout -> onLogout()
                is ProfileEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (profileState) {
                is ProfileState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF00A16B)
                    )
                }
                is ProfileState.Error -> {
                    Text(
                        text = (profileState as ProfileState.Error).message,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    // Profile display
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        profile?.let { userProfile ->
                            // Profile header with photo
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // In a real app, would load profile photo
                                // For now using a placeholder
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF00A16B).copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = userProfile.firstName.first().toString() + userProfile.lastName.first().toString(),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00A16B),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            // User name and details
                            Text(
                                text = "${userProfile.firstName} ${userProfile.lastName}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = userProfile.email,
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // User cards section
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Account Management",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 16.dp)
                            )

                            // My Cars button
                            ProfileMenuButton(
                                text = "My Cars",
                                icon = Icons.Default.DirectionsCar,
                                onClick = onNavigateToUserCars,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // My Rides button
                            ProfileMenuButton(
                                text = "My Rides",
                                icon = Icons.Default.TimeToLeave,
                                onClick = onNavigateToUserRides,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

//                            ProfileMenuButton(
//                                text = "Booking Requests",
//                                icon = Icons.Default.RequestPage,
//                                onClick = onNavigateToBookingManagement,
//                                modifier = Modifier.padding(bottom = 12.dp)
//                            )

                            // Settings
                            ProfileMenuButton(
                                text = "Settings",
                                icon = Icons.Default.Settings,
                                onClick = { /* Navigate to settings */ },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Help
                            ProfileMenuButton(
                                text = "Help",
                                icon = Icons.Default.Help,
                                onClick = { /* Navigate to help */ },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Logout button
                            Button(
                                onClick = { viewModel.logout() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A16B)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Logout",
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "Log out",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    // In a real app, you would use androidx.activity.compose.BackHandler
    // For simplicity, this is just a placeholder for the example
    LaunchedEffect(enabled) {
        // Would hook into the system back button in a real implementation
    }
}
