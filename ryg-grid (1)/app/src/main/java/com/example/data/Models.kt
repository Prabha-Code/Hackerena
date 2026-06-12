package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "junctions")
data class JunctionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val activeVehicles: Int,
    val currentPhase: String, // "GREEN", "YELLOW", "RED"
    val timeRemaining: Int,
    val congestionLevel: String, // "LOW", "MODERATE", "HIGH", "SEVERE"
    val carCount: Int,
    val bikeCount: Int,
    val busCount: Int,
    val truckCount: Int,
    val latitude: Double,
    val longitude: Double
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // "Accident Detected", "High Congestion", etc.
    val description: String, // "Silk Board Junction"
    val type: String, // "CRITICAL", "WARNING", "INFO"
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_corridors")
data class EmergencyCorridorEntity(
    @PrimaryKey val id: String, // "#EC-1021"
    val fromLocation: String,
    val toLocation: String,
    val distanceKm: Double,
    val etaMin: Int,
    val timeSavedMin: Int,
    val isActive: Boolean,
    val progress: Float // 0.0f to 1.0f progress of the ambulance
)

@Entity(tableName = "simulations")
data class SimulationEntity(
    @PrimaryKey val id: String,
    val scenarioName: String, // "Accident", "VIP Movement", "Weather Impact"
    val junctionId: String,
    val originalDelaySec: Int,
    val optimizedDelaySec: Int,
    val timestampMs: Long = System.currentTimeMillis()
)
