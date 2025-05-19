package ua.nure.rideshare.data.repository

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.dao.RouteDao
import ua.nure.rideshare.data.model.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Route data
 */
@Singleton
class RouteRepository @Inject constructor(
    private val routeDao: RouteDao
) {
    val allRoutes: Flow<List<Route>> = routeDao.getAllRoutes()

    fun getRouteById(routeId: String): Flow<Route?> = routeDao.getRouteById(routeId)

    @WorkerThread
    suspend fun insertRoute(route: Route) {
        routeDao.insertRoute(route)
    }

    @WorkerThread
    suspend fun updateRoute(route: Route) {
        routeDao.updateRoute(route)
    }

    @WorkerThread
    suspend fun deleteRoute(route: Route) {
        routeDao.deleteRoute(route)
    }

    @WorkerThread
    suspend fun deleteRouteById(routeId: String) {
        routeDao.deleteRouteById(routeId)
    }

    fun searchRoutes(query: String): Flow<List<Route>> {
        return routeDao.searchRoutes("%$query%")
    }

    fun getRoutesInArea(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<Route>> {
        return routeDao.getRoutesInArea(minLat, maxLat, minLng, maxLng)
    }

    fun getRoutesByStartAndEndArea(
        startMinLat: Double, startMaxLat: Double,
        startMinLng: Double, startMaxLng: Double,
        endMinLat: Double, endMaxLat: Double,
        endMinLng: Double, endMaxLng: Double
    ): Flow<List<Route>> {
        return routeDao.getRoutesByStartAndEndArea(
            startMinLat, startMaxLat, startMinLng, startMaxLng,
            endMinLat, endMaxLat, endMinLng, endMaxLng
        )
    }
}
