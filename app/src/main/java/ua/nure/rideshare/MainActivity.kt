package ua.nure.rideshare

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ua.nure.rideshare.ui.navigation.RideShareNavHost
import ua.nure.rideshare.ui.theme.RideShareTheme
import ua.nure.rideshare.ui.viewmodels.LocationViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Initialize the LocationViewModel using by viewModels() delegate
    private val locationViewModel: LocationViewModel by viewModels()

    // Create permission launcher using the Activity Result API
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if any location permission is granted
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationGranted) {
            // Permission is granted, inform the LocationViewModel
            locationViewModel.updateLocationPermissionStatus(true)
            // You can trigger location-dependent functionality here
        } else {
            // Permission denied, inform the LocationViewModel
            locationViewModel.updateLocationPermissionStatus(false)
            // Show a message to the user
            Toast.makeText(
                this,
                "Location permission denied. Some features may be limited.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permissions when app starts
        requestLocationPermission()

        setContent {
            RideShareTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    RideShareNavHost(
                        navController = navController,
                        locationViewModel = locationViewModel, // Use the ViewModel instance
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                locationViewModel.updateLocationPermissionStatus(true)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show explanation to the user about why you need the permission
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Required")
                    .setMessage("RideShare needs access to your location to show nearby rides and set pickup locations.")
                    .setPositiveButton("OK") { _, _ ->
                        // Launch permission request after showing rationale
                        launchLocationPermissionRequest()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        // Update ViewModel that permission is denied
                        locationViewModel.updateLocationPermissionStatus(false)
                    }
                    .create()
                    .show()
            }
            else -> {
                // Request permission directly
                launchLocationPermissionRequest()
            }
        }
    }

    private fun launchLocationPermissionRequest() {
        // Launch the permission request using the Activity Result API
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}