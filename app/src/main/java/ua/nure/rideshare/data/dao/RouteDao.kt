package ua.nure.rideshare.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.nure.rideshare.data.model.Route

/**
 * Data Access Object for Route entity
 */
@Dao
interface RouteDao {
    @Query("SELECT * FROM routes")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    fun getRouteById(routeId: String): Flow<Route?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route)

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("DELETE FROM routes WHERE routeId = :routeId")
    suspend fun deleteRouteById(routeId: String)

    @Query("SELECT * FROM routes WHERE start_location LIKE :searchQuery OR end_location LIKE :searchQuery")
    fun searchRoutes(searchQuery: String): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE start_latitude BETWEEN :minLat AND :maxLat AND start_longitude BETWEEN :minLng AND :maxLng")
    fun getRoutesInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE start_latitude BETWEEN :startMinLat AND :startMaxLat AND start_longitude BETWEEN :startMinLng AND :startMaxLng AND end_latitude BETWEEN :endMinLat AND :endMaxLat AND end_longitude BETWEEN :endMinLng AND :endMaxLng")
    fun getRoutesByStartAndEndArea(
        startMinLat: Double, startMaxLat: Double,
        startMinLng: Double, startMaxLng: Double,
        endMinLat: Double, endMaxLat: Double,
        endMinLng: Double, endMaxLng: Double
    ): Flow<List<Route>>
}
