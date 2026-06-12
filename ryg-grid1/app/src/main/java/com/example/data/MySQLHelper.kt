package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object MySQLHelper {
    private const val HOST = "mysql-1e75fe4e-akfire87-4096.f.aivencloud.com"
    private const val PORT = "20401"
    private const val DATABASE = "defaultdb"
    private const val USER = "avnadmin"
    private const val PASSWORD = "your_aiven_password_here"

    // JDBC URL with SSL settings suited for Aiven MySQL
    private val URL = "jdbc:mysql://$HOST:$PORT/$DATABASE?useSSL=true&trustServerCertificate=true&verifyServerCertificate=false"

    fun getConnection(): Connection? {
        return try {
            // Load MySQL driver
            Class.forName("com.mysql.jdbc.Driver")
            val conn = DriverManager.getConnection(URL, USER, PASSWORD)
            
            // Proactively try to create and switch to ryg_grid database
            try {
                val stmt = conn.createStatement()
                stmt.execute("CREATE DATABASE IF NOT EXISTS ryg_grid")
                stmt.execute("USE ryg_grid")
            } catch (e: Exception) {
                Log.d("MySQLHelper", "Could not create/switch database to ryg_grid, working with default defaultdb instead: ${e.message}")
            }
            conn
        } catch (e: Exception) {
            Log.e("MySQLHelper", "DB Connection failed: ${e.message}", e)
            null
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val valid = connection != null && !connection.isClosed
            Log.d("MySQLHelper", "Test Connection success: $valid")
            valid
        } catch (e: Exception) {
            Log.e("MySQLHelper", "Test connection error", e)
            false
        } finally {
            try { connection?.close() } catch (ignored: Exception) {}
        }
    }

    suspend fun setupTables() = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: return@withContext
        try {
            val statement = conn.createStatement()
            
            // 1. Create junctions
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ryg_junctions (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(100),
                    activeVehicles INT,
                    currentPhase VARCHAR(20),
                    timeRemaining INT,
                    congestionLevel VARCHAR(20),
                    carCount INT,
                    bikeCount INT,
                    busCount INT,
                    truckCount INT,
                    latitude DOUBLE,
                    longitude DOUBLE
                )
            """.trimIndent())

            // 2. Create alerts
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ryg_alerts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(100),
                    description VARCHAR(255),
                    type VARCHAR(20),
                    timestampMs BIGINT
                )
            """.trimIndent())

            // 3. Create emergency_corridors
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ryg_emergency_corridors (
                    id VARCHAR(50) PRIMARY KEY,
                    fromLocation VARCHAR(100),
                    toLocation VARCHAR(100),
                    distanceKm DOUBLE,
                    etaMin INT,
                    timeSavedMin INT,
                    isActive TINYINT(1),
                    progress FLOAT
                )
            """.trimIndent())

            // 4. Create simulations
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ryg_simulations (
                    id VARCHAR(50) PRIMARY KEY,
                    scenarioName VARCHAR(100),
                    junctionId VARCHAR(50),
                    originalDelaySec INT,
                    optimizedDelaySec INT,
                    timestampMs BIGINT
                )
            """.trimIndent())

            Log.d("MySQLHelper", "MySQL tables verified/created successfully.")
        } catch (e: Exception) {
            Log.e("MySQLHelper", "Table setup failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }

    // Insert or update a junction
    suspend fun saveJunction(j: JunctionEntity) = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: return@withContext
        val sql = """
            INSERT INTO ryg_junctions (id, name, activeVehicles, currentPhase, timeRemaining, congestionLevel, carCount, bikeCount, busCount, truckCount, latitude, longitude)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
            name = VALUES(name), activeVehicles = VALUES(activeVehicles), currentPhase = VALUES(currentPhase), 
            timeRemaining = VALUES(timeRemaining), congestionLevel = VALUES(congestionLevel), carCount = VALUES(carCount), 
            bikeCount = VALUES(bikeCount), busCount = VALUES(busCount), truckCount = VALUES(truckCount), 
            latitude = VALUES(latitude), longitude = VALUES(longitude)
        """.trimIndent()
        try {
            val pstmt = conn.prepareStatement(sql)
            pstmt.setString(1, j.id)
            pstmt.setString(2, j.name)
            pstmt.setInt(3, j.activeVehicles)
            pstmt.setString(4, j.currentPhase)
            pstmt.setInt(5, j.timeRemaining)
            pstmt.setString(6, j.congestionLevel)
            pstmt.setInt(7, j.carCount)
            pstmt.setInt(8, j.bikeCount)
            pstmt.setInt(9, j.busCount)
            pstmt.setInt(10, j.truckCount)
            pstmt.setDouble(11, j.latitude)
            pstmt.setDouble(12, j.longitude)
            pstmt.executeUpdate()
        } catch (e: Exception) {
            Log.e("MySQLHelper", "saveJunction failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }

    // Save alert
    suspend fun saveAlert(a: AlertEntity) = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: return@withContext
        val sql = "INSERT INTO ryg_alerts (title, description, type, timestampMs) VALUES (?, ?, ?, ?)"
        try {
            val pstmt = conn.prepareStatement(sql)
            pstmt.setString(1, a.title)
            pstmt.setString(2, a.description)
            pstmt.setString(3, a.type)
            pstmt.setLong(4, a.timestampMs)
            pstmt.executeUpdate()
        } catch (e: Exception) {
            Log.e("MySQLHelper", "saveAlert failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }

    // Save Corridor
    suspend fun saveCorridor(c: EmergencyCorridorEntity) = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: return@withContext
        val sql = """
            INSERT INTO ryg_emergency_corridors (id, fromLocation, toLocation, distanceKm, etaMin, timeSavedMin, isActive, progress)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
            fromLocation = VALUES(fromLocation), toLocation = VALUES(toLocation), distanceKm = VALUES(distanceKm),
            etaMin = VALUES(etaMin), timeSavedMin = VALUES(timeSavedMin), isActive = VALUES(isActive), progress = VALUES(progress)
        """.trimIndent()
        try {
            val pstmt = conn.prepareStatement(sql)
            pstmt.setString(1, c.id)
            pstmt.setString(2, c.fromLocation)
            pstmt.setString(3, c.toLocation)
            pstmt.setDouble(4, c.distanceKm)
            pstmt.setInt(5, c.etaMin)
            pstmt.setInt(6, c.timeSavedMin)
            pstmt.setBoolean(7, c.isActive)
            pstmt.setFloat(8, c.progress)
            pstmt.executeUpdate()
        } catch (e: Exception) {
            Log.e("MySQLHelper", "saveCorridor failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }

    // Save Simulation
    suspend fun saveSimulation(s: SimulationEntity) = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: return@withContext
        val sql = """
            INSERT INTO ryg_simulations (id, scenarioName, junctionId, originalDelaySec, optimizedDelaySec, timestampMs)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
            scenarioName = VALUES(scenarioName), junctionId = VALUES(junctionId), originalDelaySec = VALUES(originalDelaySec),
            optimizedDelaySec = VALUES(optimizedDelaySec), timestampMs = VALUES(timestampMs)
        """.trimIndent()
        try {
            val pstmt = conn.prepareStatement(sql)
            pstmt.setString(1, s.id)
            pstmt.setString(2, s.scenarioName)
            pstmt.setString(3, s.junctionId)
            pstmt.setInt(4, s.originalDelaySec)
            pstmt.setInt(5, s.optimizedDelaySec)
            pstmt.setLong(6, s.timestampMs)
            pstmt.executeUpdate()
        } catch (e: Exception) {
            Log.e("MySQLHelper", "saveSimulation failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
    }

    // Fetch junctions from remote mysql
    suspend fun fetchJunctions(): List<JunctionEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<JunctionEntity>()
        val conn = getConnection() ?: return@withContext list
        try {
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM ryg_junctions")
            while (rs.next()) {
                list.add(
                    JunctionEntity(
                        id = rs.getString("id"),
                        name = rs.getString("name"),
                        activeVehicles = rs.getInt("activeVehicles"),
                        currentPhase = rs.getString("currentPhase"),
                        timeRemaining = rs.getInt("timeRemaining"),
                        congestionLevel = rs.getString("congestionLevel"),
                        carCount = rs.getInt("carCount"),
                        bikeCount = rs.getInt("bikeCount"),
                        busCount = rs.getInt("busCount"),
                        truckCount = rs.getInt("truckCount"),
                        latitude = rs.getDouble("latitude"),
                        longitude = rs.getDouble("longitude")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MySQLHelper", "fetchJunctions failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
        list
    }

    // Fetch alerts
    suspend fun fetchAlerts(): List<AlertEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlertEntity>()
        val conn = getConnection() ?: return@withContext list
        try {
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM ryg_alerts ORDER BY timestampMs DESC")
            while (rs.next()) {
                list.add(
                    AlertEntity(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        description = rs.getString("description"),
                        type = rs.getString("type"),
                        timestampMs = rs.getLong("timestampMs")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MySQLHelper", "fetchAlerts failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
        list
    }

    // Fetch corridors
    suspend fun fetchCorridors(): List<EmergencyCorridorEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<EmergencyCorridorEntity>()
        val conn = getConnection() ?: return@withContext list
        try {
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM ryg_emergency_corridors")
            while (rs.next()) {
                list.add(
                    EmergencyCorridorEntity(
                        id = rs.getString("id"),
                        fromLocation = rs.getString("fromLocation"),
                        toLocation = rs.getString("toLocation"),
                        distanceKm = rs.getDouble("distanceKm"),
                        etaMin = rs.getInt("etaMin"),
                        timeSavedMin = rs.getInt("timeSavedMin"),
                        isActive = rs.getBoolean("isActive"),
                        progress = rs.getFloat("progress")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MySQLHelper", "fetchCorridors failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
        list
    }

    // Fetch simulations
    suspend fun fetchSimulations(): List<SimulationEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SimulationEntity>()
        val conn = getConnection() ?: return@withContext list
        try {
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM ryg_simulations ORDER BY timestampMs DESC")
            while (rs.next()) {
                list.add(
                    SimulationEntity(
                        id = rs.getString("id"),
                        scenarioName = rs.getString("scenarioName"),
                        junctionId = rs.getString("junctionId"),
                        originalDelaySec = rs.getInt("originalDelaySec"),
                        optimizedDelaySec = rs.getInt("optimizedDelaySec"),
                        timestampMs = rs.getLong("timestampMs")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MySQLHelper", "fetchSimulations failed: ${e.message}", e)
        } finally {
            try { conn.close() } catch (ignored: Exception) {}
        }
        list
    }

    // Push local database to Remote MySQL
    suspend fun pushLocalToRemote(
        junctions: List<JunctionEntity>,
        alerts: List<AlertEntity>,
        corridors: List<EmergencyCorridorEntity>,
        simulations: List<SimulationEntity>
    ) = withContext(Dispatchers.IO) {
        setupTables()
        junctions.forEach { saveJunction(it) }
        alerts.forEach { saveAlert(it) }
        corridors.forEach { saveCorridor(it) }
        simulations.forEach { saveSimulation(it) }
    }
}
