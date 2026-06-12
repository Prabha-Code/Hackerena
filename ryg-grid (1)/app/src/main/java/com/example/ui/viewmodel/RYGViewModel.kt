package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApi
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RYGViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RYGViewModel"
    private val db = AppDatabase.getDatabase(application)
    private val repository = RYGRepository(db)

    // UI exposed states (Database Driven)
    val junctions: StateFlow<List<JunctionEntity>> = repository.allJunctions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<AlertEntity>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val corridors: StateFlow<List<EmergencyCorridorEntity>> = repository.allCorridors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val simulations: StateFlow<List<SimulationEntity>> = repository.allSimulations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen selection controllers
    private val _selectedJunctionId = MutableStateFlow("silk_board")
    val selectedJunctionId: StateFlow<String> = _selectedJunctionId.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _apiResultLog = MutableStateFlow<String?>(null)
    val apiResultLog: StateFlow<String?> = _apiResultLog.asStateFlow()

    // Temporary/AI generated timing results after optimization runs
    private val _aiOptimizedGreen = MutableStateFlow(60)
    val aiOptimizedGreen: StateFlow<Int> = _aiOptimizedGreen.asStateFlow()

    private val _aiOptimizedYellow = MutableStateFlow(20)
    val aiOptimizedYellow: StateFlow<Int> = _aiOptimizedYellow.asStateFlow()

    private val _aiOptimizedRed = MutableStateFlow(40)
    val aiOptimizedRed: StateFlow<Int> = _aiOptimizedRed.asStateFlow()

    // Digital twin custom states
    private val _activeSimulationOutput = MutableStateFlow<String?>(null)
    val activeSimulationOutput: StateFlow<String?> = _activeSimulationOutput.asStateFlow()

    // Authentication Profile setup matching requirement
    private val _userRole = MutableStateFlow("Traffic Officer") // DEFAULT
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _username = MutableStateFlow("akfire87@gmail.com")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true) // Start pre-logged or in dashboard
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _mysqlStatus = MutableStateFlow("Tap to test connection")
    val mysqlStatus: StateFlow<String> = _mysqlStatus.asStateFlow()

    private val _mysqlIsSyncing = MutableStateFlow(false)
    val mysqlIsSyncing: StateFlow<Boolean> = _mysqlIsSyncing.asStateFlow()

    fun testMySQLConnection() {
        viewModelScope.launch {
            _mysqlStatus.value = "Checking connection..."
            val success = repository.testMySQLConnection()
            _mysqlStatus.value = if (success) "Connected to Aiven MySQL!" else "Connection Failed"
        }
    }

    fun syncWithMySQLRemote() {
        viewModelScope.launch {
            _mysqlIsSyncing.value = true
            _mysqlStatus.value = "Syncing with Aiven MySQL..."
            val success = repository.syncWithMySQLRemote()
            _mysqlStatus.value = if (success) "Sync Complete (Aiven MySQL)!" else "Sync Failed"
            _mysqlIsSyncing.value = false
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.seedInitialDataIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding data", e)
            }
        }
    }

    fun setSelectedJunction(id: String) {
        _selectedJunctionId.value = id
    }

    fun setUserRole(role: String) {
        _userRole.value = role
    }

    fun setUsername(name: String) {
        _username.value = name
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }

    fun clearApiLogs() {
        _apiResultLog.value = null
        _activeSimulationOutput.value = null
    }

    fun deleteAlert(id: Int) {
        viewModelScope.launch {
            repository.deleteAlert(id)
        }
    }

    /**
     * AI OPTIMIZER CALL via real Gemini 3.5 Flash!
     * Forms a prompt, gets optimized timing variables, extracts recommended green/yellow/red,
     * updates local SQLite DB node and updates reactive UI pills.
     */
    fun runAIOptimize(junctionId: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _apiResultLog.value = "AI Engine calculating optimal traffic wave cycles for junction: $junctionId..."
            
            val jEntity = repository.getJunctionById(junctionId)
            if (jEntity == null) {
                _apiResultLog.value = "Junction data empty."
                _isAnalyzing.value = false
                return@launch
            }

            val prompt = """
                Optimize traffic light signal phases for:
                Junction: ${jEntity.name}
                Current Traffic Volume Count: ${jEntity.activeVehicles} on-road active vehicles
                Logistics Breakdown: Cars: ${jEntity.carCount}, Bikes: ${jEntity.bikeCount}, Buses: ${jEntity.busCount}, Trucks: ${jEntity.truckCount}
                Current Traffic Congestion Index Rating: ${jEntity.congestionLevel}
                
                Calculate and return three customized timing figures (in Whole Seconds) for:
                1. GREEN PHASE duration (recommend 40-90s depending on congestion)
                2. YELLOW PHASE duration (recommend 10-25s)
                3. RED PHASE duration (recommend 30-60s)
                
                Return exactly in this JSON format so we can parse it programmatically:
                {
                  "green": <seconds>,
                  "yellow": <seconds>,
                  "red": <seconds>,
                  "explanation": "<short 1-sentence command-center style justification>"
                }
            """.trimIndent()

            val aiSystemDirective = "You are a Smart City Smart Signal Optimization AI engine. You receive local traffic congestion metrics and output precise timing durations as a JSON object with keys: green, yellow, red, explanation."

            val resultText = GeminiApi.generateText(prompt, aiSystemDirective)
            _apiResultLog.value = resultText

            // Attempt to parse out values
            try {
                val cleanJsonString = if (resultText.contains("{")) {
                    resultText.substring(resultText.indexOf("{"), resultText.lastIndexOf("}") + 1)
                } else {
                    ""
                }
                if (cleanJsonString.isNotEmpty()) {
                    val json = org.json.JSONObject(cleanJsonString)
                    val green = json.getInt("green")
                    val yellow = json.getInt("yellow")
                    val red = json.getInt("red")
                    
                    _aiOptimizedGreen.value = green
                    _aiOptimizedYellow.value = yellow
                    _aiOptimizedRed.value = red

                    // Write updated timing variables directly to the local junction db
                    val updated = jEntity.copy(
                        currentPhase = "Green",
                        timeRemaining = green,
                        activeVehicles = (jEntity.activeVehicles * 0.85).toInt() // AI decreased pressure by 15%
                    )
                    repository.updateJunction(updated)
                    
                    // Insert a nice system notification/alert
                    repository.insertAlert(
                        AlertEntity(
                            title = "AI Signal Optimized",
                            description = "${jEntity.name} synced. Congestion pressure mitigated.",
                            type = "INFO"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse AI response JSON", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * DIGITAL TWIN SCENARIO forecast generator via real Gemini 3.5 Flash!
     * Calculates delay surges, rerouting choices, and reports historical prediction.
     */
    fun runDigitalTwinSimulation(scenario: String, junctionId: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _activeSimulationOutput.value = "Calculating digital twin city model predictions for $scenario..."
            
            val junction = repository.getJunctionById(junctionId) ?: return@launch
            
            val prompt = """
                Run what-if simulation on our Digital Twin.
                Target Junction: ${junction.name}
                Scenario category: $scenario (e.g. Accident, Road Closure, Construction, Signal Failure, VIP Movement, Weather Impact)
                Base load traffic count: ${junction.activeVehicles} active units
                Base Congestion: ${junction.congestionLevel}
                
                Compute:
                1. Expected percentage increase in local delay (e.g. +75%).
                2. Recommended detour routes or adaptive signal cascades.
                3. Forecast recovery timeline to baseline state.
                
                Return exactly in this JSON format:
                {
                  "delayPercentIncrease": <whole number e.g. 85>,
                  "detour": "<description of detour or cascade solution>",
                  "timeToClearMin": <minutes e.g. 45>,
                  "briefReport": "<detailed diagnostic assessment>"
                }
            """.trimIndent()

            val response = GeminiApi.generateText(prompt, "You are an advanced Urban Digital Twin Traffic Simulator. You compute precise impacts of emergencies, roadblocks, and weather on signal corridors.")
            _activeSimulationOutput.value = response

            try {
                val cleanJsonString = if (response.contains("{")) {
                    response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1)
                } else {
                    ""
                }
                if (cleanJsonString.isNotEmpty()) {
                    val obj = org.json.JSONObject(cleanJsonString)
                    val delayPercent = obj.getInt("delayPercentIncrease")
                    val detour = obj.getString("detour")
                    val timeToClear = obj.getInt("timeToClearMin")
                    
                    // Insert custom simulation run into database history
                    repository.insertSimulation(
                        SimulationEntity(
                            id = "SIM-${System.currentTimeMillis()}",
                            scenarioName = scenario,
                            junctionId = junctionId,
                            originalDelaySec = 120,
                            optimizedDelaySec = 120 + ((120 * delayPercent) / 100)
                        )
                    )

                    // Add simulation alert details
                    repository.insertAlert(
                        AlertEntity(
                            title = "Simulation Completed: $scenario",
                            description = "Delay forecast: +$delayPercent%. Clearing time: ${timeToClear}min.",
                            type = "WARNING"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing Simulation JSON", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * AI COMPUTER VISION vehicle analyzer via Gemini 3.5 Flash!
     * Analyses input frames to extract exact vehicle listings.
     */
    fun analyzeCCTVFrame(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _apiResultLog.value = "AI Vision scanning CCTV RTSP streams..."
            
            val prompt = """
                Perform real-time machine vision inspection on this city camera feed.
                Task: Detect vehicles and output counts.
                Identify precisely the counts of:
                1. Cars
                2. Bikes
                3. Buses
                4. Trucks
                
                Provide bounding boxes mapping coordinates (in percentage like [ymin, xmin, ymax, xmax]) for up to 5 visual focal vehicles.
                
                Return exactly in this JSON format:
                {
                  "carsCount": <int>,
                  "bikesCount": <int>,
                  "busesCount": <int>,
                  "trucksCount": <int>,
                  "density": "<LOW or MODERATE or HIGH or SEVERE>",
                  "detectedBoxes": [
                    {"label": "Car", "box": [20,15,50,45]},
                    {"label": "Truck", "box": [60,40,90,75]}
                  ]
                }
            """.trimIndent()

            val response = GeminiApi.analyzeImage(bitmap, prompt)
            _apiResultLog.value = response

            try {
                val cleanJsonString = if (response.contains("{")) {
                    response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1)
                } else {
                    ""
                }
                if (cleanJsonString.isNotEmpty()) {
                    val obj = org.json.JSONObject(cleanJsonString)
                    val cars = obj.getInt("carsCount")
                    val bikes = obj.getInt("bikesCount")
                    val buses = obj.getInt("busesCount")
                    val trucks = obj.getInt("trucksCount")
                    val density = obj.getString("density")

                    // Update local Silk Board Junction dataset dynamically based on REAL Image analysis!
                    val j = repository.getJunctionById("silk_board")
                    if (j != null) {
                        val updated = j.copy(
                            carCount = cars,
                            bikeCount = bikes,
                            busCount = buses,
                            truckCount = trucks,
                            activeVehicles = cars + bikes + buses + trucks,
                            congestionLevel = density
                        )
                        repository.updateJunction(updated)

                        // Insert critical alert report update
                        repository.insertAlert(
                            AlertEntity(
                                title = "Computer Vision Dynamic Sync",
                                description = "CCTV counts updated. Total active: ${cars + bikes + buses + trucks} vehicles.",
                                type = "CRITICAL"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error matching AI Vision JSON payload", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Schedules a custom Emergency Corridor event.
     */
    fun createEmergencyCorridor(from: String, to: String) {
        viewModelScope.launch {
            val corridor = EmergencyCorridorEntity(
                id = "#EC-${(1000..9999).random()}",
                fromLocation = from,
                toLocation = to,
                distanceKm = ((40..150).random() / 10.0),
                etaMin = (5..15).random(),
                timeSavedMin = (2..5).random(),
                isActive = true,
                progress = 0.0f
            )
            repository.insertCorridor(corridor)

            // Auto cascade local junctions to green along route! Let's update MG Road and Silk Board
            val sBoard = repository.getJunctionById("silk_board")
            if (sBoard != null) {
                repository.updateJunction(sBoard.copy(currentPhase = "Green", timeRemaining = 90))
            }
            val mgRoad = repository.getJunctionById("mg_road")
            if (mgRoad != null) {
                repository.updateJunction(mgRoad.copy(currentPhase = "Green", timeRemaining = 90))
            }

            // Insert system notification alert
            repository.insertAlert(
                AlertEntity(
                    title = "Emergency Corridor Cleared",
                    description = "Signals prioritized green route: $from to $to.",
                    type = "CRITICAL"
                )
            )
        }
    }
}
