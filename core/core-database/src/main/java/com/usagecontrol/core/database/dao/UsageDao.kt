package com.usagecontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.usagecontrol.core.database.entity.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: UsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(usages: List<UsageEntity>)

    @Query("SELECT * FROM usage_records WHERE date = :date ORDER BY foregroundTimeMs DESC")
    fun getByDateFlow(date: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_records WHERE date = :date ORDER BY foregroundTimeMs DESC")
    suspend fun getByDate(date: String): List<UsageEntity>

    @Query("""
        SELECT * FROM usage_records
        WHERE packageName = :packageName
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    suspend fun getByPackageAndDateRange(
        packageName: String,
        startDate: String,
        endDate: String,
    ): List<UsageEntity>

    @Query("""
        SELECT SUM(foregroundTimeMs) FROM usage_records
        WHERE packageName = :packageName AND date = :date
    """)
    suspend fun getTotalTimeForAppOnDate(packageName: String, date: String): Long?

    // Top apps by average daily usage in the date range
    @Query("""
        SELECT packageName, appName, AVG(foregroundTimeMs) as avgDailyMs,
            SUM(foregroundTimeMs) as totalMs, COUNT(*) as daysUsed
        FROM usage_records
        WHERE date >= :startDate
        GROUP BY packageName
        HAVING avgDailyMs > :minAvgMs
        ORDER BY avgDailyMs DESC
    """)
    suspend fun getHeavyUsageApps(startDate: String, minAvgMs: Long): List<HeavyUsageRow>

    @Query("DELETE FROM usage_records WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String): Int
}

data class HeavyUsageRow(
    val packageName: String,
    val appName: String,
    val avgDailyMs: Long,
    val totalMs: Long,
    val daysUsed: Int,
)
