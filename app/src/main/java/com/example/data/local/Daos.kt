package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)
}

@Dao
interface RoadDao {
    @Query("SELECT * FROM roads")
    fun getAllRoads(): Flow<List<RoadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoads(roads: List<RoadEntity>)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY riskPercentage DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE status = 'ACTIVE' ORDER BY riskPercentage DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("DELETE FROM alerts")
    suspend fun clearAlerts()
}

@Dao
interface HistoricalDao {
    @Query("SELECT * FROM historical_events ORDER BY eventDate DESC")
    fun getAllHistoricalEvents(): Flow<List<HistoricalEventEntity>>

    @Query("SELECT * FROM historical_events ORDER BY eventDate DESC")
    suspend fun getHistoricalEventsList(): List<HistoricalEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalEvents(events: List<HistoricalEventEntity>)
}

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions ORDER BY timestamp DESC LIMIT 50")
    fun getRecentPredictions(): Flow<List<PredictionEntity>>

    @Insert
    suspend fun insertPrediction(prediction: PredictionEntity)
}
