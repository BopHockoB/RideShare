package ua.nure.rideshare.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ua.nure.rideshare.data.db.RideShareDatabase
import ua.nure.rideshare.data.dao.*
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing database and DAO dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): RideShareDatabase {
        return RideShareDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(database: RideShareDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideProfileDao(database: RideShareDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideCarDao(database: RideShareDatabase): CarDao {
        return database.carDao()
    }

    @Provides
    fun provideRouteDao(database: RideShareDatabase): RouteDao {
        return database.routeDao()
    }

    @Provides
    fun provideTripDao(database: RideShareDatabase): TripDao {
        return database.tripDao()
    }

    @Provides
    fun provideTripBookingDao(database: RideShareDatabase): TripBookingDao {
        return database.tripBookingDao()
    }
}
