package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationEntity::class,
        RoadEntity::class,
        RainfallEntity::class,
        WaterLevelEntity::class,
        DrainageEntity::class,
        PredictionEntity::class,
        AlertEntity::class,
        HistoricalEventEntity::class,
        RoutePredictionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FloodSafeDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun roadDao(): RoadDao
    abstract fun alertDao(): AlertDao
    abstract fun historicalDao(): HistoricalDao
    abstract fun predictionDao(): PredictionDao

    companion object {
        @Volatile
        private var INSTANCE: FloodSafeDatabase? = null

        fun getDatabase(context: Context): FloodSafeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FloodSafeDatabase::class.java,
                    "floodsafe_bengaluru.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
