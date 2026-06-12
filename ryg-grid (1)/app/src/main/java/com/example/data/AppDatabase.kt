package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JunctionDao {
    @Query("SELECT * FROM junctions")
    fun getAllJunctionsFlow(): Flow<List<JunctionEntity>>

    @Query("SELECT * FROM junctions WHERE id = :id")
    suspend fun getJunctionById(id: String): JunctionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(junctions: List<JunctionEntity>)

    @Update
    suspend fun updateJunction(junction: JunctionEntity)

    @Query("DELETE FROM junctions")
    suspend fun clearAll()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestampMs DESC")
    fun getAllAlertsFlow(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("DELETE FROM alerts")
    suspend fun clearAlerts()

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)
}

@Dao
interface EmergencyCorridorDao {
    @Query("SELECT * FROM emergency_corridors")
    fun getAllCorridorsFlow(): Flow<List<EmergencyCorridorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorridor(corridor: EmergencyCorridorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(corridors: List<EmergencyCorridorEntity>)

    @Update
    suspend fun updateCorridor(corridor: EmergencyCorridorEntity)

    @Query("DELETE FROM emergency_corridors")
    suspend fun clearAll()
}

@Dao
interface SimulationDao {
    @Query("SELECT * FROM simulations ORDER BY timestampMs DESC")
    fun getAllSimulationsFlow(): Flow<List<SimulationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: SimulationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(simulations: List<SimulationEntity>)

    @Query("DELETE FROM simulations")
    suspend fun clearAll()
}

@Database(
    entities = [
        JunctionEntity::class,
        AlertEntity::class,
        EmergencyCorridorEntity::class,
        SimulationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun junctionDao(): JunctionDao
    abstract fun alertDao(): AlertDao
    abstract fun emergencyCorridorDao(): EmergencyCorridorDao
    abstract fun simulationDao(): SimulationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ryg_grid_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
