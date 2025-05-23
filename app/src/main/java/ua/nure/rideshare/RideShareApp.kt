package ua.nure.rideshare

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for RideShare
 * This enables Hilt for dependency injection and initializes Places SDK
 */
@HiltAndroidApp
class RideShareApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Places SDK with API key
        // The API key is defined in AndroidManifest.xml as com.google.android.geo.API_KEY
        val apiKey = applicationContext.getString(R.string.google_maps_key)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
    }
}