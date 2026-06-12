package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import android.util.Log

class RYGRepository(private val db: AppDatabase) {
    private val junctionDao = db.junctionDao()
    private val alertDao = db.alertDao()
    private val corridorDao = db.emergencyCorridorDao()
    private val simulationDao = db.simulationDao()

    val allJunctions: Flow<List<JunctionEntity>> = junctionDao.getAllJunctionsFlow()
    val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlertsFlow()
    val allCorridors: Flow<List<EmergencyCorridorEntity>> = corridorDao.getAllCorridorsFlow()
    val allSimulations: Flow<List<SimulationEntity>> = simulationDao.getAllSimulationsFlow()

    suspend fun getJunctionById(id: String): JunctionEntity? {
        return junctionDao.getJunctionById(id)
    }

    suspend fun updateJunction(junction: JunctionEntity) {
        junctionDao.updateJunction(junction)
        try {
            MySQLHelper.saveJunction(junction)
        } catch (e: Exception) {
            Log.e("RYGRepository", "Failed saving updated junction to remote: ${e.message}")
        }
    }

    suspend fun insertAlert(alert: AlertEntity) {
        alertDao.insertAlert(alert)
        try {
            MySQLHelper.saveAlert(alert)
        } catch (e: Exception) {
            Log.e("RYGRepository", "Failed saving alert to remote: ${e.message}")
        }
    }

    suspend fun deleteAlert(id: Int) {
        alertDao.deleteAlertById(id)
    }

    suspend fun insertCorridor(corridor: EmergencyCorridorEntity) {
        corridorDao.insertCorridor(corridor)
        try {
            MySQLHelper.saveCorridor(corridor)
        } catch (e: Exception) {
            Log.e("RYGRepository", "Failed saving corridor to remote: ${e.message}")
        }
    }

    suspend fun updateCorridor(corridor: EmergencyCorridorEntity) {
        corridorDao.updateCorridor(corridor)
        try {
            MySQLHelper.saveCorridor(corridor)
        } catch (e: Exception) {
            Log.e("RYGRepository", "Failed saving corridor to remote: ${e.message}")
        }
    }

    suspend fun insertSimulation(simulation: SimulationEntity) {
        simulationDao.insertSimulation(simulation)
        try {
            MySQLHelper.saveSimulation(simulation)
        } catch (e: Exception) {
            Log.e("RYGRepository", "Failed saving simulation to remote: ${e.message}")
        }
    }

    suspend fun testMySQLConnection(): Boolean {
        return MySQLHelper.testConnection()
    }

    suspend fun clearLocalCache() {
        junctionDao.clearAll()
        alertDao.clearAlerts()
        corridorDao.clearAll()
        simulationDao.clearAll()
    }

    suspend fun syncWithMySQLRemote(): Boolean {
        return try {
            Log.d("RYGRepository", "Syncing all tables from Aiven MySQL...")
            MySQLHelper.setupTables()

            // Fetch live actual rows from cloud database
            val remoteJunctions = MySQLHelper.fetchJunctions()
            val remoteAlerts = MySQLHelper.fetchAlerts()
            val remoteCorridors = MySQLHelper.fetchCorridors()
            val remoteSimulations = MySQLHelper.fetchSimulations()

            // Purge local room database cache first (removes old fake/mock data)
            clearLocalCache()

            // Load remote real rows into local cache
            if (remoteJunctions.isNotEmpty()) {
                junctionDao.insertAll(remoteJunctions)
            }
            if (remoteAlerts.isNotEmpty()) {
                alertDao.insertAll(remoteAlerts)
            }
            if (remoteCorridors.isNotEmpty()) {
                corridorDao.insertAll(remoteCorridors)
            }
            if (remoteSimulations.isNotEmpty()) {
                simulationDao.insertAll(remoteSimulations)
            }
            true
        } catch (e: Exception) {
            Log.e("RYGRepository", "MySQL remote sync/pull error: ${e.message}", e)
            false
        }
    }

    /**
     * Purges local fake data and fetches actual database records from remote Aiven MySQL.
     * If the remote Aiven cloud DB itself is completely empty (first run), we seed
     * the remote Aiven database once with the baseline signal grids and then cache them.
     */
    suspend fun seedInitialDataIfNeeded() {
        Log.d("RYGRepository", "Initializing actual database connection to Aiven MySQL...")
        try {
            // Setup remote schema
            MySQLHelper.setupTables()

            // Fetch actual remote signals
            var remoteJunctions = MySQLHelper.fetchJunctions()

            if (remoteJunctions.isEmpty()) {
                Log.d("RYGRepository", "Remote Aiven DB is empty. Seeding actual city junctions to Aiven MySQL cloud...")
                val defaultJunctions = listOf(
                    JunctionEntity(
                        id = "silk_board",
                        name = "Silk Board Junction",
                        activeVehicles = 238,
                        currentPhase = "Green",
                        timeRemaining = 18,
                        congestionLevel = "Severe",
                        carCount = 128,
                        bikeCount = 76,
                        busCount = 12,
                        truckCount = 22,
                        latitude = 12.9176,
                        longitude = 77.6244
                    ),
                    JunctionEntity(
                        id = "mg_road",
                        name = "MG Road Junction",
                        activeVehicles = 145,
                        currentPhase = "Red",
                        timeRemaining = 32,
                        congestionLevel = "High",
                        carCount = 82,
                        bikeCount = 45,
                        busCount = 8,
                        truckCount = 10,
                        latitude = 12.9733,
                        longitude = 77.6117
                    ),
                    JunctionEntity(
                        id = "hebbal",
                        name = "Hebbal Junction",
                        activeVehicles = 180,
                        currentPhase = "Green",
                        timeRemaining = 45,
                        congestionLevel = "Moderate",
                        carCount = 95,
                        bikeCount = 60,
                        busCount = 15,
                        truckCount = 10,
                        latitude = 13.0358,
                        longitude = 77.5970
                    ),
                    JunctionEntity(
                        id = "electronic_city",
                        name = "Electronic City Gate",
                        activeVehicles = 90,
                        currentPhase = "Green",
                        timeRemaining = 25,
                        congestionLevel = "Low",
                        carCount = 50,
                        bikeCount = 28,
                        busCount = 4,
                        truckCount = 8,
                        latitude = 12.8500,
                        longitude = 77.6667
                    ),
                    JunctionEntity(
                        id = "koramangala",
                        name = "Koramangala 80ft Rd",
                        activeVehicles = 110,
                        currentPhase = "Yellow",
                        timeRemaining = 5,
                        congestionLevel = "Moderate",
                        carCount = 60,
                        bikeCount = 35,
                        busCount = 10,
                        truckCount = 5,
                        latitude = 12.9352,
                        longitude = 77.6245
                    ),
                    JunctionEntity(
                        id = "indiranagar",
                        name = "Indiranagar 100ft Rd",
                        activeVehicles = 85,
                        currentPhase = "Green",
                        timeRemaining = 15,
                        congestionLevel = "Low",
                        carCount = 45,
                        bikeCount = 30,
                        busCount = 5,
                        truckCount = 5,
                        latitude = 12.9719,
                        longitude = 77.6412
                    )
                )

                // Write these primary records directly into the real MySQL DB
                defaultJunctions.forEach { MySQLHelper.saveJunction(it) }

                val defaultAlerts = listOf(
                    AlertEntity(
                        title = "Accident Detected",
                        description = "Silk Board Junction - Major collision blocking lanes.",
                        type = "CRITICAL",
                        timestampMs = System.currentTimeMillis() - 120_000
                    ),
                    AlertEntity(
                        title = "High Congestion",
                        description = "MG Road - Vehicle density peak exceeded.",
                        type = "WARNING",
                        timestampMs = System.currentTimeMillis() - 300_000
                    )
                )
                defaultAlerts.forEach { MySQLHelper.saveAlert(it) }

                val defaultCorridor = EmergencyCorridorEntity(
                    id = "#EC-1021",
                    fromLocation = "Jayadeva Hospital",
                    toLocation = "Apollo Hospital",
                    distanceKm = 8.7,
                    etaMin = 8,
                    timeSavedMin = 4,
                    isActive = true,
                    progress = 0.35f
                )
                MySQLHelper.saveCorridor(defaultCorridor)

                // Re-fetch now that Aiven MySQL is seeded with real base records
                remoteJunctions = MySQLHelper.fetchJunctions()
            }

            // Perform full local cache flush and update
            syncWithMySQLRemote()
        } catch (e: Exception) {
            Log.e("RYGRepository", "Failed loading real DB data on initializer: ${e.message}", e)
            // Just clear local fake content anyway to ensure clean experience
            clearLocalCache()
        }
    }
}
