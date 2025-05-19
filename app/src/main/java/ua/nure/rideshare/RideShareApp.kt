package ua.nure.rideshare

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for RideShare
 * This enables Hilt for dependency injection
 */
@HiltAndroidApp
class RideShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-wide services or configurations here
    }
}