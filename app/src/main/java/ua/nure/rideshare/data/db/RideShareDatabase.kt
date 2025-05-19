package ua.nure.rideshare.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ua.nure.rideshare.data.dao.*
import ua.nure.rideshare.data.model.*

/**
 * Room database for the RideShare application
 */
@Database(
    entities = [
        User::class,
        Profile::class,
        Car::class,
        Route::class,
        Trip::class,
        TripBooking::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RideShareDatabase : RoomDatabase() {

    /**
     * Get the UserDao
     */
    abstract fun userDao(): UserDao

    /**
     * Get the ProfileDao
     */
    abstract fun profileDao(): ProfileDao

    /**
     * Get the CarDao
     */
    abstract fun carDao(): CarDao

    /**
     * Get the RouteDao
     */
    abstract fun routeDao(): RouteDao

    /**
     * Get the TripDao
     */
    abstract fun tripDao(): TripDao

    /**
     * Get the TripBookingDao
     */
    abstract fun tripBookingDao(): TripBookingDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: RideShareDatabase? = null

        /**
         * Get the database instance
         */
        fun getDatabase(context: Context): RideShareDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideShareDatabase::class.java,
                    "rideshare_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object specified
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
