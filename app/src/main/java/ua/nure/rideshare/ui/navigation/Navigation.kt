package ua.nure.rideshare.ui.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ua.nure.rideshare.data.model.Car
import ua.nure.rideshare.data.model.Profile
import ua.nure.rideshare.data.model.Route
import ua.nure.rideshare.data.model.Trip
import ua.nure.rideshare.ui.screens.auth.LoginScreen
import ua.nure.rideshare.ui.screens.auth.RegisterScreen
import ua.nure.rideshare.ui.screens.booking.BookingManagementScreen
import ua.nure.rideshare.ui.screens.booking.BookingRequestScreen
import ua.nure.rideshare.ui.screens.car.CarCreationScreen
import ua.nure.rideshare.ui.screens.car.UserCarsScreen
import ua.nure.rideshare.ui.screens.home.HomeScreen
import ua.nure.rideshare.ui.screens.profile.ProfileScreen
import ua.nure.rideshare.ui.screens.ride.RideCreationScreen
import ua.nure.rideshare.ui.screens.ride.RideDetailsScreen
import ua.nure.rideshare.ui.screens.ride.YourRideScreen
import ua.nure.rideshare.ui.screens.search.SearchScreen
import ua.nure.rideshare.ui.viewmodels.AuthViewModel
import ua.nure.rideshare.ui.viewmodels.LocationViewModel
import ua.nure.rideshare.ui.viewmodels.RideViewModel
import ua.nure.rideshare.ui.viewmodels.SearchViewModel

/**
 * Sealed class defining all navigation routes in the app
 */
sealed class Screen(val route: String) {
    /**
     * Onboarding screen shown to first-time users
     */
    object Onboarding : Screen("onboarding")

    /**
     * Login screen
     */
    object Login : Screen("login")

    /**
     * Registration screen
     */
    object Register : Screen("register")

    /**
     * Main home screen with map
     */
    object Home : Screen("home")

    /**
     * Search screen for finding rides
     * Accepts optional origin and destination parameters
     */
    object Search : Screen("search?origin={origin}&destination={destination}") {
        fun createRoute(origin: String = "", destination: String = ""): String {
            return "search?origin=$origin&destination=$destination"
        }
    }

    object BookRideRequest : Screen("book_request/{tripId}") {
        fun createRoute(tripId: String): String {
            return "book_request/$tripId"
        }
    }

    object BookingManagement : Screen("booking_management")

    /**
     * Ride details screen
     * Shows information about a specific ride
     */
    object RideDetails : Screen("ride/{rideId}") {
        fun createRoute(rideId: String): String {
            return "ride/$rideId"
        }
    }

    /**
     * Booking screen for a specific ride
     */


    /**
     * Ride creation screen for drivers
     */
    object CreateRide : Screen("create_ride?carId={carId}") {
        fun createRoute(carId: String? = null): String {
            return if (carId != null) "create_ride?carId=$carId" else "create_ride"
        }
    }

    /**
     * User profile screen
     */
    object Profile : Screen("profile")

    /**
     * Chat/messaging screen
     */
    object Chats : Screen("chats")

    /**
     * Screen showing user's booked/offered rides
     */
    object YourRides : Screen("your_rides")

    /**
     * Screen showing user's cars
     */
    object UserCars : Screen("user_cars")

    /**
     * Screen for adding a new car
     */
    object AddCar : Screen("add_car")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RideShareNavHost(
    navController: NavHostController,
    locationViewModel: LocationViewModel,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var selectedCarIdForRide by remember { mutableStateOf<String?>(null) }

    // Collect the current user ID from authViewModel
    LaunchedEffect(authViewModel) {
        authViewModel.currentUserId.collect { userId ->
            currentUserId = userId
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { userId ->
                    // Save userId and navigate to home
                    currentUserId = userId
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = { userId ->
                    currentUserId = userId
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                locationViewModel = locationViewModel,
                onNavigateToSearch = { origin, destination ->
                    navController.navigate(Screen.Search.createRoute(origin, destination))
                },
                onNavigateToRideDetails = { rideId ->
                    navController.navigate(Screen.RideDetails.createRoute(rideId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToChats = {
                    navController.navigate(Screen.Chats.route)
                },
                onNavigateToBookingManagement = {
                    navController.navigate(Screen.BookingManagement.route)
                },
                onNavigateToYourRides = {
                    navController.navigate(Screen.YourRides.route)
                },
                onNavigateToCreateRide = {
                    // First navigate to user cars to select a car
                    navController.navigate(Screen.UserCars.route)
                }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("origin") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("destination") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val origin = backStackEntry.arguments?.getString("origin") ?: ""
            val destination = backStackEntry.arguments?.getString("destination") ?: ""

            SearchScreen(
                locationViewModel = locationViewModel,
                origin = origin,
                destination = destination,
                onNavigateToRideDetails = { rideId ->
                    navController.navigate(Screen.RideDetails.createRoute(rideId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RideDetails.route,
            arguments = listOf(
                navArgument("rideId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId") ?: ""

            RideDetailsScreen(
                rideId = rideId,
                onBackClick = { navController.popBackStack() },
                onBookClick = {
                    // For now, navigate to the existing BookRide route
                    // Later you can change this to BookRideRequest when you implement it
                    navController.navigate(Screen.BookRideRequest.createRoute(rideId))
                }
            )
        }

        composable(
            route = Screen.CreateRide.route, // "create_ride?carId={carId}"
            arguments = listOf(
                navArgument("carId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            currentUserId?.let { userId ->
                val carId = backStackEntry.arguments?.getString("carId")

                // Add logging to debug
                Log.d("NAVIGATION", "CreateRide - Received carId: $carId")

                RideCreationScreen(
                    userId = userId,
                    locationViewModel = locationViewModel,
                    selectedCarId = carId,
                    onBackClick = { navController.popBackStack() },
                    onConfirmTrip = { startLocation, endLocation, date, time, vehicle ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.CreateRide.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.Profile.route) {
            currentUserId?.let { userId ->
                ProfileScreen(
                    userId = userId,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    },
                    onNavigateToUserCars = {
                        navController.navigate(Screen.UserCars.route)
                    },
                    onNavigateToUserRides = {
                        navController.navigate(Screen.YourRides.route)
                    },
                    onLogout = {
                        // When logging out, reset currentUserId and navigate back to login
                        currentUserId = null
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                    // viewModel parameter has default value hiltViewModel(), so no need to pass it explicitly
                )
            } ?: run {
                // If userId is null, navigate back to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }

        // User Cars screen
        composable(Screen.UserCars.route) {
            currentUserId?.let { userId ->
                UserCarsScreen(
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddNewCar = { navController.navigate(Screen.AddCar.route) },
                    onSelectCar = { carId ->
                        // Store the selected car ID
                        selectedCarIdForRide = carId

                        Log.d("NAVIGATION", "UserCars - Selected carId: $carId")

                        // Navigate to create ride with the car ID
                        navController.navigate(Screen.CreateRide.createRoute(carId)) {
                            // Don't pop the UserCars screen, so user can go back
                            // popUpTo(Screen.UserCars.route) { inclusive = true }
                        }
                    }
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }

        // Add Car screen
        composable(Screen.AddCar.route) {
            currentUserId?.let { userId ->
                CarCreationScreen(
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onCarCreated = { carId ->
                        // After creating a car, navigate back to user cars
                        navController.navigate(Screen.UserCars.route) {
                            popUpTo(Screen.AddCar.route) { inclusive = true }
                        }
                    }
                )
            } ?: run {
                // If userId is null, navigate back to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }

        // Placeholder for Chats screen
        composable(Screen.Chats.route) {
            // ChatScreen would go here when implemented
            // For now, we'll just use a simple placeholder
            Surface(modifier = Modifier.fillMaxSize()) {
                Text("Chats Screen - Coming Soon")
            }
        }

        // Your Rides screen - UPDATED: Now uses the actual YourRideScreen
        composable(Screen.YourRides.route) {
            currentUserId?.let { userId ->
                YourRideScreen(
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateNewRide = {
                        // Navigate to user cars to select a car first
                        navController.navigate(Screen.UserCars.route)
                    },
                    onRideDetails = { rideId ->
                        navController.navigate(Screen.RideDetails.createRoute(rideId))
                    },
                    onNavigateToBookingManagement = {
                        navController.navigate(Screen.BookingManagement.route)
                    }
                )
            } ?: run {
                // If userId is null, navigate back to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }

        composable(
            route = Screen.RideDetails.route,
            arguments = listOf(
                navArgument("rideId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId") ?: ""

            RideDetailsScreen(
                rideId = rideId,
                onBackClick = { navController.popBackStack() },
                onBookClick = {
                    // Navigate to booking request screen with trip data
                    navController.navigate(Screen.BookRideRequest.createRoute(rideId))
                }
            )
        }

// Add the new BookingRequestScreen composable
        composable(
            route = Screen.BookRideRequest.route,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""

            currentUserId?.let { userId ->
                // We need to load trip data here since we're not passing it through navigation
                val rideViewModel: RideViewModel = hiltViewModel()
                val searchViewModel: SearchViewModel = hiltViewModel()

                var trip by remember { mutableStateOf<Trip?>(null) }
                var route by remember { mutableStateOf<Route?>(null) }
                var driverProfile by remember { mutableStateOf<Profile?>(null) }
                var car by remember { mutableStateOf<Car?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(tripId) {
                    try {
                        val (tripData, routeData) = rideViewModel.getTripWithRoute(tripId)
                        trip = tripData
                        route = routeData

                        // Load additional data
                        searchViewModel.loadPopularTrips()
                        searchViewModel.searchResults.collect { results ->
                            val matchingResult = results.find { it.trip.tripId == tripId }
                            if (matchingResult != null) {
                                driverProfile = matchingResult.driverProfile
                                car = matchingResult.car
                                isLoading = false
                                return@collect
                            }
                        }
                    } catch (e: Exception) {
                        isLoading = false
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00A16B))
                    }
                } else {
                    BookingRequestScreen(
                        tripId = tripId,
                        userId = userId,
                        trip = trip,
                        route = route,
                        driverProfile = driverProfile,
                        car = car,
                        onBackClick = { navController.popBackStack() },
                        onBookingCreated = {
                            navController.navigate(Screen.BookingManagement.route) {
                                popUpTo(Screen.BookRideRequest.route) { inclusive = true }
                            }
                        },
                        locationViewModel = locationViewModel
                    )
                }
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }

// Add BookingManagement screen
        composable(Screen.BookingManagement.route) {
            currentUserId?.let { userId ->
                BookingManagementScreen(
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTripDetails = { tripId ->
                        navController.navigate(Screen.RideDetails.createRoute(tripId))
                    }
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
    }
}