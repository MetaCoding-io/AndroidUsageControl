package com.usagecontrol.core.database

import android.util.Log
import androidx.room.withTransaction
import com.usagecontrol.core.database.dao.LaneDao
import com.usagecontrol.core.database.dao.RouteDao
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RouteKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Lanes and routes (R014). Route changes re-file matching history in one transaction. */
@Singleton
class LaneRepository @Inject constructor(
    private val db: UsageControlDatabase,
    private val laneDao: LaneDao,
    private val routeDao: RouteDao,
) {
    val lanes: Flow<List<Lane>> = laneDao.getAllFlow().map { rows -> rows.map { it.toModel() } }

    val routes: Flow<List<Route>> = routeDao.getAllFlow().map { rows -> rows.mapNotNull { it.toModelOrNull() } }

    suspend fun getLanes(): List<Lane> = laneDao.getAll().map { it.toModel() }

    suspend fun getLane(id: Long): Lane? = laneDao.getById(id)?.toModel()

    suspend fun getRoutes(): List<Route> = routeDao.getAll().mapNotNull { it.toModelOrNull() }

    suspend fun getRoutesFor(packageName: String): List<Route> =
        routeDao.getForPackage(packageName).mapNotNull { it.toModelOrNull() }

    fun routesFor(packageName: String): Flow<List<Route>> =
        routeDao.getForPackageFlow(packageName).map { rows -> rows.mapNotNull { it.toModelOrNull() } }

    /** Inserts when [Lane.id] is 0, otherwise updates. Returns the lane id. */
    suspend fun saveLane(lane: Lane): Long =
        if (lane.id == 0L) laneDao.insert(lane.toEntity()) else lane.id.also { laneDao.update(lane.toEntity()) }

    /**
     * Deletes a custom lane; default lanes are kept. Routes pointing at it fall back
     * to the app/category default on next resolution.
     */
    suspend fun deleteLane(id: Long): Boolean = db.withTransaction {
        val removed = laneDao.deleteCustom(id) > 0
        if (removed) routeDao.getAll().filter { it.laneId == id }.forEach {
            routeDao.delete(it.kind, it.packageName, it.key)
        }
        removed
    }

    /**
     * Creates or replaces the route for (kind, packageName, key) and re-files that
     * sender's history into [laneId]. Returns the number of notification rows re-filed.
     */
    suspend fun setRoute(
        kind: RouteKind,
        packageName: String,
        key: String?,
        laneId: Long,
        now: Long = System.currentTimeMillis(),
    ): Int {
        require(kind == RouteKind.APP || !key.isNullOrEmpty()) { "$kind route needs a key" }
        val route = Route(kind = kind, packageName = packageName, key = key.orEmpty(), laneId = laneId, createdAt = now)
        val reFiled = db.withTransaction {
            routeDao.upsert(route.toEntity())
            routeDao.reFileHistory(packageName, route.toEntity().key, laneId, kind.name)
        }
        Log.i(TAG, "route set kind=$kind pkg=$packageName key=${route.key} lane=$laneId reFiled=$reFiled")
        return reFiled
    }

    suspend fun removeRoute(kind: RouteKind, packageName: String, key: String?): Boolean =
        routeDao.delete(kind.name, packageName, if (kind == RouteKind.APP) "" else key.orEmpty()) > 0

    companion object {
        const val TAG = "Lanes"
    }
}
