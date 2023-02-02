package cloud.mindbox.mobile_sdk.monitoring.data.room.dao

import androidx.room.*
import cloud.mindbox.mobile_sdk.monitoring.data.room.entities.MonitoringEntity


@Dao
internal interface MonitoringDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entity: MonitoringEntity)

    @Query("DELETE FROM monitoring WHERE id IN (SELECT id FROM monitoring ORDER BY id ASC LIMIT (SELECT CNT/10 FROM (SELECT COUNT(id) as CNT FROM monitoring)))")
    suspend fun deleteFirstTenPercentOfLogs()

    @Delete
    suspend fun deleteLog(log: MonitoringEntity)

    @Query("DELETE FROM monitoring WHERE id IN (SELECT id FROM monitoring ORDER BY id ASC LIMIT 1)")
    suspend fun deleteFirstLog()

    @Query("SELECT * FROM monitoring WHERE timestamp BETWEEN :startTime and :endTime ORDER BY timestamp ASC")
    suspend fun getLogs(startTime: String, endTime: String): List<MonitoringEntity>

    @Query("SELECT * FROM monitoring ORDER BY id ASC LIMIT 1")
    suspend fun getFirstLog(): MonitoringEntity

    @Query("SELECT * FROM monitoring ORDER BY id DESC LIMIT 1")
    suspend fun getLastLog(): MonitoringEntity
}