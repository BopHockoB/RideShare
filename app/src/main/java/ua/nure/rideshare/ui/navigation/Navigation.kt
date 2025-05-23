package ua.nure.rideshare.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ua.nure.rideshare.ui.screens.auth.LoginScreen
import ua.nure.rideshare.ui.screens.auth.RegisterScreen
import ua.nure.rideshare.ui.screens.car.CarCreationScreen
import ua.nure.rideshare.ui.screens.car.UserCarsScreen
import ua.nure.rideshare.ui.screens.home.HomeScreen
import ua.nure.rideshare.ui.screens.profile.ProfileScreen
import ua.nure.rideshare.ui.screens.ride.RideCreationScreen
import ua.nure.rideshare.ui.screens.ride.RideDetailsScreen
import ua.nure.rideshare.ui.screens.search.SearchScreen
import ua.nure.rideshare.ui.viewmodels.AuthViewModel
import ua.nure.rideshare.ui.viewmodels.LocationViewModel

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
    object BookRide : Screen("book/{rideId}") {
        fun createRoute(rideId: String): String {
            return "book/$rideId"
        }
    }

    /**
     * Ride creation screen for drivers
     */
    object CreateRide : Screen("create_ride") {
        fun createRoute(): String {
            return "create_ride"
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
                onBookClick = { navController.navigate(Screen.BookRide.createRoute(rideId)) }
            )
        }

        composable(
            route = Screen.BookRide.route,
            arguments = listOf(
                navArgument("rideId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId") ?: ""

            // Placeholder for BookRideScreen - will be implemented later
            Surface(modifier = Modifier.fillMaxSize()) {
                Text("Book Ride Screen - Coming Soon")
            }
        }

        composable(Screen.CreateRide.route) {
            currentUserId?.let { userId ->
                RideCreationScreen(
                    locationViewModel = locationViewModel,
                    onBackClick = { navController.popBackStack() },
                    onConfirmTrip = { startLocation, endLocation, date, time, vehicle ->
                        // After creating a ride, navigate back to home
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
                    onLogout = {
                        // When logging out, reset currentUserId and navigate back to login
                        currentUserId = null
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
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

        // User Cars screen
        composable(Screen.UserCars.route) {
            currentUserId?.let { userId ->
                UserCarsScreen(
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddNewCar = { navController.navigate(Screen.AddCar.route) },
                    onSelectCar = { carId ->
                        // When a car is selected for a ride, navigate to create ride
                        navController.navigate(Screen.CreateRide.route) {
                            // You might want to pass the carId to CreateRide screen
                            // For now, this is simplified
                            popUpTo(Screen.UserCars.route) { inclusive = true }
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

        // Placeholder for Your Rides screen
        composable(Screen.YourRides.route) {
            // YourRidesScreen would go here when implemented
            // For now, we'll just use a simple placeholder
            Surface(modifier = Modifier.fillMaxSize()) {
                Text("Your Rides Screen - Coming Soon")
            }
        }
    }
}
