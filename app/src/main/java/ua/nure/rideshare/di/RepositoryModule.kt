package ua.nure.rideshare.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ua.nure.rideshare.data.dao.*
import ua.nure.rideshare.data.repository.*
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing repository dependencies
 *
 * Create this file as: app/src/main/java/ua/nure/rideshare/di/RepositoryModule.kt
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideUserRepository(
        userDao: UserDao,
        profileDao: ProfileDao
    ): UserRepository {
        return UserRepository(userDao, profileDao)
    }

    @Singleton
    @Provides
    fun provideTripRepository(
        tripDao: TripDao,
        tripBookingDao: TripBookingDao
    ): TripRepository {
        return TripRepository(tripDao, tripBookingDao)
    }

    @Singleton
    @Provides
    fun provideRouteRepository(
        routeDao: RouteDao
    ): RouteRepository {
        return RouteRepository(routeDao)
    }

    @Singleton
    @Provides
    fun provideCarRepository(
        carDao: CarDao
    ): CarRepository {
        return CarRepository(carDao)
    }
}