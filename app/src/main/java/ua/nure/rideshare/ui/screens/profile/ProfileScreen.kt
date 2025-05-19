package ua.nure.rideshare.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import ua.nure.rideshare.ui.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToHome: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    userId: String = "current_user_id" // In a real app, this would come from auth service
) {
    // Local context for showing toasts
    val context = LocalContext.current

    // UI state
    var isEditMode by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Form state (for editing)
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var drivingExperience by remember { mutableStateOf("") }
    var conversationStyle by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(listOf<String>()) }

    // Collect profile state
    val profileState by viewModel.profileState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val profile by viewModel.profile.collectAsState()

    // Load profile on start
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    // Update form fields when profile changes
    LaunchedEffect(profile) {
        profile?.let {
            firstName = it.firstName
            lastName = it.lastName
            email = it.email
            phoneNumber = it.phoneNumber
            gender = it.gender
            age = it.age.toString()
            bio = it.bio ?: ""
            drivingExperience = it.drivingExperience?.toString() ?: ""
            conversationStyle = it.conversationStyle ?: "Chatty"
            // Interests would come from a separate table in a real app
            interests = listOf("Music", "Travel", "Technology")
        }
    }

    // Handle one-time events
    LaunchedEffect(key1 = true) {
        viewModel.profileEvent.collectLatest { event ->
            when (event) {
                is ProfileEvent.ProfileUpdated -> {
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    isEditMode = false
                }
                is ProfileEvent.Logout -> {
                    onLogout()
                }
                is ProfileEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        SmallTopAppBar(
            title = { Text("Profile") },
            navigationIcon = {
                IconButton(onClick = { /* Open drawer */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu"
                    )
                }
            },
            actions = {
                if (!isEditMode) {
                    IconButton(onClick = { isEditMode = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Profile"
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            // Save changes
                            try {
                                val ageInt = age.toIntOrNull() ?: 0
                                val drivingExpInt = drivingExperience.toIntOrNull()

                                viewModel.updateProfile(
                                    firstName = firstName,
                                    lastName = lastName,
                                    phoneNumber = phoneNumber,
                                    gender = gender,
                                    age = ageInt,
                                    bio = bio.takeIf { it.isNotBlank() },
                                    drivingExperience = drivingExpInt
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = editState != EditState.Saving
                    ) {
                        if (editState == EditState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF00A16B)
                            )
                        } else {
                            Text("Save", color = Color(0xFF00A16B))
                        }
                    }
                }
            }
        )

        when (profileState) {
            is ProfileState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00A16B))
                }
            }
            is ProfileState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (profileState as ProfileState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is ProfileState.Success, is ProfileState.Initial -> {
                // Profile Photo Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        // Profile Image
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(
                                    BorderStroke(4.dp, Color(0xFF00A16B)),
                                    CircleShape
                                )
                                .background(Color.LightGray)
                        ) {
                            // Replace with actual image loading
                            // For now, we'll use a placeholder
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.Center)
                            )
                        }

                        // Edit photo button
                        if (isEditMode) {
                            IconButton(
                                onClick = { /* Handle photo upload */ },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00A16B))
                                    .align(Alignment.BottomEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Photo",
                                    tint = Color.White
                                )
                            }
                        } else {
                            // Online status indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00A16B))
                                    .align(Alignment.BottomEnd)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Online",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // User Full Name
                Text(
                    text = "$firstName $lastName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                // Profile Fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Email
                    ProfileField(
                        label = "Email",
                        value = email,
                        readOnly = true, // Email should not be editable
                        onValueChange = { email = it }
                    )

                    // Phone Number
                    PhoneNumberField(
                        value = phoneNumber,
                        readOnly = !isEditMode,
                        onValueChange = { phoneNumber = it }
                    )

                    // Gender Dropdown
                    DropdownField(
                        label = "Gender",
                        value = gender,
                        readOnly = !isEditMode,
                        options = listOf("MALE", "FEMALE"),
                        onValueChange = { gender = it }
                    )

                    // Age
                    ProfileField(
                        label = "Age",
                        value = age,
                        readOnly = !isEditMode,
                        onValueChange = { age = it }
                    )

                    // Driving Experience
                    ProfileField(
                        label = "Driving Experience (years)",
                        value = drivingExperience,
                        readOnly = !isEditMode,
                        onValueChange = { drivingExperience = it }
                    )

                    // Conversation Style Dropdown
                    DropdownField(
                        label = "Conversation Style",
                        value = conversationStyle,
                        readOnly = !isEditMode,
                        options = listOf("Chatty", "Quiet", "Professional", "Friendly"),
                        onValueChange = { conversationStyle = it }
                    )

                    // Bio
                    ProfileField(
                        label = "Bio",
                        value = bio,
                        readOnly = !isEditMode,
                        onValueChange = { bio = it },
                        singleLine = false,
                        minLines = 3
                    )

                    // Interests
                    if (isEditMode) {
                        Text(
                            text = "Interests",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            interests.forEach { interest ->
                                InterestChip(
                                    interest = interest,
                                    selected = true,
                                    onRemove = {
                                        interests = interests.filter { it != interest }
                                    }
                                )
                            }

                            // Add interest button
                            OutlinedButton(
                                onClick = { /* Open add interest dialog */ },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF00A16B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add interest",
                                    tint = Color(0xFF00A16B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add",
                                    color = Color(0xFF00A16B),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        if (interests.isNotEmpty()) {
                            Text(
                                text = "Interests",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                interests.forEach { interest ->
                                    InterestChip(interest = interest, selected = true)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Logout Button
                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00A16B))
                    ) {
                        Text(
                            text = "Logout",
                            color = Color(0xFF00A16B),
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Bottom Navigation Bar
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = Color.White,
            contentColor = Color(0xFF00A16B)
        ) {
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home"
                    )
                },
                label = {
                    Text(
                        "Home",
                        fontSize = 12.sp
                    )
                },
                selected = false,
                onClick = { onNavigateToHome() }
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chats"
                    )
                },
                label = {
                    Text(
                        "Chats",
                        fontSize = 12.sp
                    )
                },
                selected = false,
                onClick = { /* Navigate to chats */ }
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Your rides"
                    )
                },
                label = {
                    Text(
                        "Your rides",
                        fontSize = 12.sp
                    )
                },
                selected = false,
                onClick = { /* Navigate to rides */ }
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color(0xFF00A16B)
                    )
                },
                label = {
                    Text(
                        "Profile",
                        color = Color(0xFF00A16B),
                        fontSize = 12.sp
                    )
                },
                selected = true,
                onClick = { /* Already on profile */ }
            )
        }
    }
}


@Composable
fun ProfileField(
    label: String,
    value: String,
    readOnly: Boolean,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00A16B),
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = Color(0xFF00A16B),
            cursorColor = Color(0xFF00A16B)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun PhoneNumberField(
    value: String,
    readOnly: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text("Phone Number") },
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // Ukrainian flag indicator
                Box(
                    modifier = Modifier
                        .size(24.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF0057B7))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFFFFD700))
                            .align(Alignment.BottomCenter)
                    )
                }

                if (!readOnly) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select country",
                        tint = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00A16B),
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = Color(0xFF00A16B),
            cursorColor = Color(0xFF00A16B)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    readOnly: Boolean,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { /* Handled by dropdown */ },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                if (!readOnly) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        modifier = Modifier.clickable { expanded = !expanded }
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
                .clickable(enabled = !readOnly) { expanded = true }
        )

        if (expanded && !readOnly) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.TopStart)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InterestChip(
    interest: String,
    selected: Boolean,
    onRemove: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0x1A00A16B) else Color.LightGray.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (selected) Color(0xFF00A16B) else Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = interest,
                color = if (selected) Color(0xFF00A16B) else Color.DarkGray,
                fontSize = 14.sp
            )

            if (onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove interest",
                    tint = if (selected) Color(0xFF00A16B) else Color.DarkGray,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onRemove() }
                )
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit  // Notice the RowScope receiver
) {
    // This is a simple implementation of FlowRow
    // In a real app, you would use a more robust implementation
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}