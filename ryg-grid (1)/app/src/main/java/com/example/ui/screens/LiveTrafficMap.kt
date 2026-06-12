package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveTrafficMapScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val junctions by viewModel.junctions.collectAsState()
    val selectedJunctionId by viewModel.selectedJunctionId.collectAsState()
    val activeAlerts by viewModel.alerts.collectAsState()
    val corridors by viewModel.corridors.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var activeLayerType by remember { mutableStateOf("hybrid") } // Default: Satellite Hybrid
    var isDigitalTwinMode by remember { mutableStateOf(false) }

    var systemGpsCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Capture mobile GPS details
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else {
                    LocationManager.NETWORK_PROVIDER
                }

                val initialLoc = locationManager.getLastKnownLocation(provider)
                if (initialLoc != null) {
                    systemGpsCoordinates = Pair(initialLoc.latitude, initialLoc.longitude)
                }

                locationManager.requestLocationUpdates(
                    provider,
                    5000L,
                    10f,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            systemGpsCoordinates = Pair(location.latitude, location.longitude)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        if (granted) {
            Toast.makeText(context, "Satellite GPS Lock Active", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Using default command centers markers", Toast.LENGTH_SHORT).show()
        }
    }

    val activeJunctionOnMap = remember(junctions, selectedJunctionId) {
        junctions.find { it.id == selectedJunctionId } ?: junctions.firstOrNull()
    }

    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Unified Robust State Synchronizer for Leaflet Map WebView
    LaunchedEffect(isPageLoaded, junctions, selectedJunctionId, activeLayerType, isDigitalTwinMode, searchQuery, selectedFilterCategory, systemGpsCoordinates) {
        val webView = webViewRef
        if (isPageLoaded && webView != null) {
            // 1. Sync Map Layer selection
            webView.evaluateJavascript("setMapLayer('$activeLayerType');", null)
            
            // 2. Sync Digital Twin Blueprints Mode
            webView.evaluateJavascript("setDigitalTwinMode($isDigitalTwinMode);", null)
            
            // 3. Sync Active Search Query
            webView.evaluateJavascript("setSearchQuery('${searchQuery.replace("'", "\\'")}');", null)
            
            // 4. Sync Category Chip Filtering
            webView.evaluateJavascript("selectCategoryFilter('${selectedFilterCategory}');", null)
            
            // 5. Sync Smartphone GPS Overlay
            systemGpsCoordinates?.let { (lat, lng) ->
                webView.evaluateJavascript("updateUserLocation($lat, $lng);", null)
            }
            
            // 6. Bulk update junctions JSON payload
            val jsonArray = org.json.JSONArray()
            junctions.forEach { j ->
                val obj = org.json.JSONObject().apply {
                    put("id", j.id)
                    put("name", j.name)
                    put("latitude", j.latitude)
                    put("longitude", j.longitude)
                    put("congestionLevel", j.congestionLevel)
                    put("activeVehicles", j.activeVehicles)
                    put("currentPhase", j.currentPhase.uppercase())
                    put("timeRemaining", j.timeRemaining)
                }
                jsonArray.put(obj)
            }
            val javascriptCall = "updateJunctions('${jsonArray.toString().replace("'", "\\'")}');"
            webView.evaluateJavascript(javascriptCall, null)

            // 7. Center viewport on the selected node
            activeJunctionOnMap?.let { j ->
                webView.evaluateJavascript("centerOnJunction(${j.latitude}, ${j.longitude});", null)
            }
        }
    }

    // Premium interactive HTML map using high-fidelity offline vector SVG city blueprints matching the user screenshot exactly with zero CDN downloads.
    val commandCenterHTML = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />
            <style>
                body, html { 
                    margin: 0; padding: 0; width: 100vw; height: 100vh; display: flex; flex-direction: column; 
                    background-color: #0d1210; overflow: hidden; 
                    font-family: -apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; 
                    user-select: none; -webkit-user-select: none;
                }
                #map-container { 
                    flex: 1; min-height: 100%; width: 100vw; height: 100vh; 
                    position: absolute; top: 0; left: 0; z-index: 1; 
                    background-color: #0b0f0d; touch-action: none;
                }
                #map { 
                    position: absolute; top: 0; left: 0; width: 100%; height: 100%; 
                    transform-origin: center center; cursor: grab; 
                }
                #map:active { cursor: grabbing; }

                /* Map Loader Style matching premium dark-satellite telemetry flow */
                #map-loader {
                    position: absolute; top: 0; left: 0; width: 100vw; height: 100vh;
                    background-color: #090c0b; display: flex; flex-direction: column;
                    align-items: center; justify-content: center; z-index: 9999;
                    color: #00E5FF; transition: opacity 0.4s ease-out;
                }
                .spinner {
                    width: 44px; height: 44px; border: 4px solid rgba(0, 229, 255, 0.15);
                    border-top: 4px solid #00E5FF; border-radius: 50%;
                    animation: spin 1s linear infinite; margin-bottom: 12px;
                }
                @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                
                /* Digital Twin Blueprint Neon Inversion Filter overlay */
                .digital-twin-active {
                    filter: hue-rotate(180deg) saturate(2.0) brightness(1.2) contrast(1.1) !important;
                }
            </style>
        </head>
        <body>
            <div id="map-loader">
                <div class="spinner"></div>
                <div style="font-size: 11px; font-weight: bold; letter-spacing: 2px;">SYNCING HIGH-RES TELEMETRY GRID...</div>
            </div>
            
            <div id="map-container">
                <svg id="map" viewBox="0 0 1000 1000">
                    <defs>
                        <!-- Neon glows for paths -->
                        <filter id="glow-green" x="-20%" y="-20%" width="140%" height="140%">
                            <feGaussianBlur stdDeviation="8" result="blur" />
                            <feMerge>
                                <feMergeNode in="blur" />
                                <feMergeNode in="SourceGraphic" />
                            </feMerge>
                        </filter>
                        <filter id="glow-yellow" x="-20%" y="-20%" width="140%" height="140%">
                            <feGaussianBlur stdDeviation="8" result="blur" />
                            <feMerge>
                                <feMergeNode in="blur" />
                                <feMergeNode in="SourceGraphic" />
                            </feMerge>
                        </filter>
                        <filter id="glow-red" x="-20%" y="-20%" width="140%" height="140%">
                            <feGaussianBlur stdDeviation="10" result="blur" />
                            <feMerge>
                                <feMergeNode in="blur" />
                                <feMergeNode in="SourceGraphic" />
                            </feMerge>
                        </filter>

                        <!-- Dark rich backgrounds -->
                        <radialGradient id="satellite-glow" cx="50%" cy="50%" r="70%">
                            <stop offset="0%" stop-color="#141c18" />
                            <stop offset="60%" stop-color="#0b100e" />
                            <stop offset="100%" stop-color="#050807" />
                        </radialGradient>
                        
                        <radialGradient id="blueprint-grid" cx="50%" cy="50%" r="70%">
                            <stop offset="0%" stop-color="#061224" />
                            <stop offset="100%" stop-color="#02070f" />
                        </radialGradient>

                        <!-- Subtle building layout clusters pattern to match screenshot high-fidelity look -->
                        <pattern id="grid-pattern" width="60" height="60" patternUnits="userSpaceOnUse">
                            <rect width="60" height="60" fill="none" />
                            <path d="M 60 0 L 0 0 0 60" fill="none" stroke="rgba(255, 255, 255, 0.05)" stroke-width="0.5" />
                        </pattern>
                    </defs>

                    <!-- Viewport containing everything which scales & translates smoothly on zoom/drag -->
                    <g id="map-viewport">
                        <rect id="map-bg" width="1000" height="1000" fill="url(#satellite-glow)" />
                        <rect width="1000" height="1000" fill="url(#grid-pattern)" />

                        <!-- Forested & Park shapes matching Green Park shape on left side of image -->
                        <path d="M 30,120 Q 150,110 180,240 T 160,520 Q 100,580 40,480 T 30,120 Z" fill="#13231a" opacity="0.85" stroke="#1d3427" stroke-width="2" />
                        <text x="110" y="310" fill="#22C55E" font-size="14px" font-weight="bold" opacity="0.45" letter-spacing="1px">GREEN PARK</text>

                        <!-- Satellite building clusters representation (subtle block layouts) -->
                        <g fill="rgba(255,255,255,0.03)" stroke="rgba(255,255,255,0.06)" stroke-width="0.5">
                            <rect x="330" y="90" width="30" height="25" />
                            <rect x="380" y="80" width="45" height="35" />
                            <rect x="440" y="100" width="30" height="30" />
                            <rect x="690" y="120" width="50" height="40" />
                            <rect x="760" y="100" width="35" height="30" />
                            <rect x="230" y="340" width="40" height="40" />
                            <rect x="290" y="360" width="30" height="45" />
                            <rect x="520" y="340" width="45" height="30" />
                            <rect x="580" y="350" width="35" height="35" />
                            <rect x="420" y="590" width="50" height="40" />
                            <rect x="490" y="600" width="35" height="30" />
                            <rect x="580" y="620" width="45" height="45" />
                            <rect x="140" y="700" width="45" height="35" />
                            <rect x="190" y="760" width="40" height="40" />
                            <rect x="640" y="790" width="45" height="35" />
                            <rect x="710" y="810" width="35" height="30" />
                        </g>

                        <!-- Dynamic Connecting Road Corridors - Lane base stroke (Deep Obsidian Lines) -->
                        <g stroke="rgba(255, 255, 255, 0.12)" stroke-width="12" fill="none" stroke-linecap="round" stroke-linejoin="round">
                            <path id="base-mg-central" d="M 250 180 L 440 470" />
                            <path id="base-central-silk" d="M 440 470 L 280 710" />
                            <path id="base-silk-koramangala" d="M 280 710 L 410 800" />
                            <path id="base-central-koramangala" d="M 440 470 L 410 800" />
                            <path id="base-central-citycenter" d="M 440 470 L 600 410" />
                            <path id="base-central-civic" d="M 440 470 L 580 170" />
                            <path id="base-mg-civic" d="M 250 180 L 580 170" />
                            <path id="base-silk-jayadeva" d="M 280 710 L 800 740" />
                        </g>

                        <!-- Dynamic glowing overlay lines representing live congestion speeds -->
                        <g stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round">
                            <path id="flow-mg-central" d="M 250 180 L 440 470" stroke="#10B981" filter="url(#glow-green)" />
                            <path id="flow-central-silk" d="M 440 470 L 280 710" stroke="#EF4444" filter="url(#glow-red)" />
                            <path id="flow-silk-koramangala" d="M 280 710 L 410 800" stroke="#10B981" filter="url(#glow-green)" />
                            <path id="flow-central-koramangala" d="M 440 470 L 410 800" stroke="#F59E0B" filter="url(#glow-yellow)" />
                            <path id="flow-central-citycenter" d="M 440 470 L 600 410" stroke="#EF4444" filter="url(#glow-red)" />
                            <path id="flow-central-civic" d="M 440 470 L 580 170" stroke="#10B981" filter="url(#glow-green)" />
                            <path id="flow-mg-civic" d="M 250 180 L 580 170" stroke="#10B981" filter="url(#glow-green)" />
                            <path id="flow-silk-jayadeva" d="M 280 710 L 800 740" stroke="#EF4444" filter="url(#glow-red)" />
                        </g>

                        <!-- Road/District text labels styled precisely like screen overlays -->
                        <g fill="#FFFFFF" opacity="0.6" font-family="-apple-system, sans-serif" font-weight="950" font-size="11px" letter-spacing="1.5px">
                            <text x="580" y="150" text-anchor="middle">Civil Lines</text>
                            <text x="610" y="440" text-anchor="middle">City Center</text>
                            <text x="410" y="830" text-anchor="middle">Koramangala</text>
                        </g>

                        <!-- Active emergency routes overlays -->
                        <!-- AI Detour routing (dotted orange line) -->
                        <path id="ai-route-overlay" d="M 250 180 Q 500 240, 580 170 T 800 740" stroke="#F97316" stroke-width="4.5" stroke-dasharray="10,6" fill="none" opacity="0.8" />
                        <!-- Emergency Green Corridor (glowing green pathway) -->
                        <path id="emergency-route-overlay" d="M 250 180 L 440 470 L 280 710" stroke="#22C55E" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round" fill="none" opacity="0.9" />

                        <!-- Dynamic Markers layer (repopulated continuously by JavaScript) -->
                        <g id="markers-layer"></g>

                        <!-- Smartphone GPS Indicator -->
                        <g id="user-location-marker" style="display:none;">
                            <circle id="user-pulse" cx="0" cy="0" r="16" fill="rgba(0, 229, 255, 0.25)">
                                <animate attributeName="r" values="8;24;8" dur="1.5s" repeatCount="indefinite" />
                            </circle>
                            <circle id="user-core" cx="0" cy="0" r="8" fill="#00E5FF" stroke="#FFFFFF" stroke-width="2" />
                        </g>
                    </g>
                </svg>
            </div>

            <script>
                var mapContainer = document.getElementById('map-container');
                var mapSvg = document.getElementById('map');
                var viewport = document.getElementById('map-viewport');

                // Configuration Cache State
                var activeLayerTypeName = "hybrid";
                var twinModeEnabled = false;
                var currentQuery = "";
                var currentCategory = "All";
                var userLatLng = null;
                var junctionsCache = [];
                var selectedJunctionId = "";

                // Static Coordinate Anchor Lookup (Ensuring perfect overlay matching layout icons)
                var junctionAnchors = {
                    "silk_board": { x: 280, y: 710, name: "Silk Board Junction" },
                    "mg_road": { x: 250, y: 180, name: "MG Road" },
                    "hebbal": { x: 580, y: 170, name: "Civil Lines" },
                    "indiranagar": { x: 735, y: 181, name: "Indiranagar" },
                    "koramangala": { x: 410, y: 800, name: "Koramangala" },
                    "electronic_city": { x: 800, y: 840, name: "Electronic City Gate" },
                    "central_junction": { x: 440, y: 470, name: "Central Junction" }
                };

                // Hospitals dataset
                var hospitals = [
                    { name: "Apollo Hospital", lat: 12.9420, lng: 77.5950, icon: "🏥", id: "apollo" },
                    { name: "Jayadeva Hospital", lat: 12.9200, lng: 77.6530, icon: "🏥", id: "jayadeva" },
                    { name: "St. John's Super Speciality", lat: 12.9329, lng: 77.6200, icon: "🏥", id: "st_johns" },
                    { name: "Manipal Hospital Indiranagar", lat: 12.9620, lng: 77.6430, icon: "🏥", id: "manipal" }
                ];

                // Incidents dataset
                var incidents = [
                    { name: "⚠ Multi-Vehicle Crash", lat: 12.9185, lng: 77.6250, desc: "Silk Board Overpass - 2 Lanes Blocked", id: "accident_silk" },
                    { name: "🚧 Emergency Asphalt Repair", lat: 13.0180, lng: 77.5955, desc: "Hebbal Overpass - Slow construction", id: "road_closure_hebbal" }
                ];

                // Emergency class
                var specialVehicles = [
                    { name: "Ambulance 🚑 [CONNECTED EMS]", lat: 12.9600, lng: 77.6150, type: "Ambulance", id: "amb_1" },
                    { name: "Preemptive Fire Engine 🚒 [STANDBY]", lat: 12.9710, lng: 77.6390, type: "Fire", id: "fire_1" },
                    { name: "Police Patrol 🚓 [ACTIVE]", lat: 12.9740, lng: 77.6100, type: "Police", id: "police_1" }
                ];

                // Converts any coordinate to the 1000x1000 SVG space layout
                function latLngToXY(lat, lng) {
                    var x = ((lng - 77.575) / (77.665 - 77.575)) * 1000;
                    var y = 1000 - ((lat - 12.89) / (12.99 - 12.89)) * 1000;
                    
                    if (x < 15) x = 15;
                    if (x > 985) x = 985;
                    if (y < 15) y = 15;
                    if (y > 985) y = 985;
                    
                    return { x: x, y: y };
                }

                // TOUCH GESTURES (Double touch pinch-to-zoom & Single touch drag-to-pan)
                var isDragging = false;
                var startX, startY;
                var translateX = 0, translateY = 0;
                var scale = 1.0;
                var lastTouchDistance = 0;

                mapContainer.onmousedown = function(e) {
                    isDragging = true;
                    startX = e.clientX - translateX;
                    startY = e.clientY - translateY;
                };

                mapContainer.onmousemove = function(e) {
                    if (!isDragging) return;
                    translateX = e.clientX - startX;
                    translateY = e.clientY - startY;
                    clampAndApplyTransform();
                };

                mapContainer.onmouseup = function() { isDragging = false; };
                mapContainer.onmouseleave = function() { isDragging = false; };

                mapContainer.ontouchstart = function(e) {
                    if (e.touches.length === 1) {
                        isDragging = true;
                        startX = e.touches[0].clientX - translateX;
                        startY = e.touches[0].clientY - translateY;
                    } else if (e.touches.length === 2) {
                        isDragging = false;
                        lastTouchDistance = getTouchDistance(e);
                    }
                };

                mapContainer.ontouchmove = function(e) {
                    if (e.touches.length === 1) {
                        if (!isDragging) return;
                        translateX = e.touches[0].clientX - startX;
                        translateY = e.touches[0].clientY - startY;
                        clampAndApplyTransform();
                    } else if (e.touches.length === 2) {
                        var dist = getTouchDistance(e);
                        if (lastTouchDistance > 0) {
                            var factor = dist / lastTouchDistance;
                            scale *= factor;
                            if (scale < 0.6) scale = 0.6;
                            if (scale > 3.5) scale = 3.5;
                            clampAndApplyTransform();
                        }
                        lastTouchDistance = dist;
                    }
                };

                mapContainer.ontouchend = function() {
                    isDragging = false;
                    lastTouchDistance = 0;
                };

                function getTouchDistance(e) {
                    var dx = e.touches[0].clientX - e.touches[1].clientX;
                    var dy = e.touches[0].clientY - e.touches[1].clientY;
                    return Math.sqrt(dx * dx + dy * dy);
                }

                function clampAndApplyTransform() {
                    // Prevent dragging components outside readable range
                    var boundary = 600 * scale;
                    if (translateX < -boundary) translateX = -boundary;
                    if (translateX > boundary) translateX = boundary;
                    if (translateY < -boundary) translateY = -boundary;
                    if (translateY > boundary) translateY = boundary;
                    
                    viewport.setAttribute('transform', 'translate(' + translateX + ',' + translateY + ') scale(' + scale + ')');
                }

                // API Bridges connecting to Jetpack Compose UI actions
                function initMap() {
                    setTimeout(hideLoader, 400);
                }

                function hideLoader() {
                    var loader = document.getElementById('map-loader');
                    if (loader) {
                        loader.style.opacity = '0';
                        setTimeout(function() { loader.style.display = 'none'; }, 400);
                    }
                }

                function setMapLayer(layer) {
                    activeLayerTypeName = layer;
                    var bg = document.getElementById('map-bg');
                    if (!bg) return;
                    
                    if (layer === 'standard') {
                        bg.setAttribute('fill', '#0e1624'); // Steel Slate Blue Blueprint
                    } else if (layer === 'satellite' || layer === 'hybrid') {
                        bg.setAttribute('fill', 'url(#satellite-glow)');
                    } else if (layer === 'digital_twin') {
                        bg.setAttribute('fill', 'url(#blueprint-grid)');
                    }
                }

                function setDigitalTwinMode(enabled) {
                    twinModeEnabled = enabled;
                    var map = document.getElementById('map');
                    var bg = document.getElementById('map-bg');
                    if (enabled) {
                        map.classList.add('digital-twin-active');
                        bg.setAttribute('fill', 'url(#blueprint-grid)');
                        startTwinSimulation();
                    } else {
                        map.classList.remove('digital-twin-active');
                        bg.setAttribute('fill', 'url(#satellite-glow)');
                        stopTwinSimulation();
                    }
                }

                function zoomIn() {
                    scale *= 1.25;
                    if (scale > 3.5) scale = 3.5;
                    clampAndApplyTransform();
                }

                var activeJunctionIdOnScroll = "";

                function zoomOut() {
                    scale /= 1.25;
                    if (scale < 0.6) scale = 0.6;
                    clampAndApplyTransform();
                }

                function centerOnJunction(lat, lng) {
                    var pos = latLngToXY(lat, lng);
                    
                    // Smoothly transition center
                    var rect = mapContainer.getBoundingClientRect();
                    var centerX = rect.width / 2;
                    var centerY = rect.height / 2;
                    
                    translateX = centerX - pos.x * scale;
                    translateY = centerY - pos.y * scale;
                    clampAndApplyTransform();
                }

                function onNodeSelect(id) {
                    selectedJunctionId = id;
                    renderAllElements();
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onJunctionSelected(id);
                    }
                }

                function toast(msg) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.toastMessage(msg);
                    }
                }

                function updateJunctions(junctionsJson) {
                    try {
                        junctionsCache = JSON.parse(junctionsJson);
                    } catch(e) {
                        console.error("JSON parse failure in cache:", e);
                    }
                    renderAllElements();
                }

                function selectCategoryFilter(category) {
                    currentCategory = category;
                    renderAllElements();
                }

                function setSearchQuery(query) {
                    currentQuery = query.toLowerCase();
                    renderAllElements();
                }

                function updateUserLocation(lat, lng) {
                    userLatLng = [lat, lng];
                    var marker = document.getElementById('user-location-marker');
                    if (!marker) return;

                    var pos = latLngToXY(lat, lng);
                    marker.setAttribute('transform', 'translate(' + pos.x + ',' + pos.y + ')');
                    marker.style.display = 'block';
                }

                // Main Render loop regenerating SVG entities dynamically
                function renderAllElements() {
                    var layer = document.getElementById('markers-layer');
                    if (!layer) return;
                    layer.innerHTML = ''; // Fresh clean render

                    // 1. RENDER SIGNAL NODES
                    junctionsCache.forEach(function(j) {
                        if (currentCategory !== "All" && currentCategory !== "Signals" && currentCategory !== "Traffic") return;
                        if (currentQuery !== "" && !j.name.toLowerCase().includes(currentQuery)) return;

                        // Retrieve optimized standard coordinate x/y layout mapping
                        var pos = junctionAnchors[j.id];
                        if (!pos) {
                            pos = latLngToXY(j.latitude, j.longitude);
                        }

                        var phaseColor = "#10B981"; // GREEN
                        if (j.currentPhase.toUpperCase() === "RED") phaseColor = "#EF4444";
                        else if (j.currentPhase.toUpperCase() === "YELLOW") phaseColor = "#F59E0B";

                        var isSelected = (selectedJunctionId === j.id);
                        var markerHtml = '';

                        // Selection pulsing outline
                        if (isSelected) {
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="26" fill="none" stroke="#FD802E" stroke-width="2" stroke-dasharray="4,4"><animateTransform attributeName="transform" type="rotate" from="0 '+pos.x+' '+pos.y+'" to="360 '+pos.x+' '+pos.y+'" dur="8s" repeatCount="indefinite"/></circle>';
                        }

                        // Base element
                        markerHtml += '<g cursor="pointer" onclick="onNodeSelect(\''+j.id+'\')">';
                        
                        // Glow core and backing
                        markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="16" fill="#141917" stroke="'+phaseColor+'" stroke-width="3" style="filter: drop-shadow(0 0 6px '+phaseColor+');"/>';
                        
                        // Signal light emoji inside
                        markerHtml += '<text x="'+pos.x+'" y="'+(pos.y + 4)+'" font-size="11px" text-anchor="middle" pointer-events="none">🚦</text>';

                        // Overlay name text plate
                        markerHtml += '<g opacity="0.9">';
                        markerHtml += '<rect x="'+(pos.x - 50)+'" y="'+(pos.y - 32)+'" width="100" height="14" rx="3" fill="#0d110f" stroke="'+phaseColor+'" stroke-width="0.5"/>';
                        markerHtml += '<text x="'+pos.x+'" y="'+(pos.y - 22)+'" font-size="8px" fill="#FFFFFF" font-weight="900" text-anchor="middle" pointer-events="none">'+j.name+'</text>';
                        markerHtml += '</g>';

                        markerHtml += '</g>';
                        layer.innerHTML += markerHtml;
                    });

                    // 2. RENDER HOSPITALS (Matching locations perfectly from user screenshot)
                    if (currentCategory === "All" || currentCategory === "Hospitals") {
                        hospitals.forEach(function(h) {
                            if (currentQuery !== "" && !h.name.toLowerCase().includes(currentQuery)) return;

                            var pos = latLngToXY(h.lat, h.lng);
                            if (h.id === "apollo") { pos = { x: 150, y: 530 }; }
                            if (h.id === "jayadeva") { pos = { x: 800, y: 740 }; }
                            if (h.id === "manipal") { pos = { x: 735, y: 181 }; }
                            if (h.id === "st_johns") { pos = { x: 410, y: 790 }; }

                            var markerHtml = '<g cursor="pointer" onclick="toast(\'' + h.name + ' EMS base unit online.\')">';
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="15" fill="none" stroke="#3DB2FF" stroke-width="1.5"/>';
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="11" fill="#1E3E54" stroke="#3DB2FF" stroke-width="2" style="filter: drop-shadow(0 0 6px #3DB2FF);" />';
                            markerHtml += '<text x="'+pos.x+'" y="'+(pos.y+4.5)+'" font-size="11px" font-weight="950" fill="#3DB2FF" text-anchor="middle">H</text>';
                            markerHtml += '<text x="'+pos.x+'" y="'+(pos.y + 24)+'" font-size="9px" font-weight="bold" fill="#3DB2FF" text-anchor="middle">'+h.name+'</text>';
                            markerHtml += '</g>';

                            layer.innerHTML += markerHtml;
                        });
                    }

                    // 3. RENDER INCIDENTS
                    if (currentCategory === "All" || currentCategory === "Incidents") {
                        incidents.forEach(function(i) {
                            if (currentQuery !== "" && !i.name.toLowerCase().includes(currentQuery)) return;

                            var pos = latLngToXY(i.lat, i.lng);
                            if (i.id.includes("silk")) { pos = { x: 320, y: 640 }; }
                            if (i.id.includes("hebbal")) { pos = { x: 520, y: 230 }; }

                            var emoji = i.id.includes("closure") ? "🚧" : "🚨";
                            var color = i.id.includes("closure") ? "#F59E0B" : "#EF4444";

                            var markerHtml = '<g cursor="pointer" onclick="toast(\'' + i.name + ': ' + i.desc + '\')">';
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="14" fill="none" stroke="'+color+'" stroke-width="1.5" stroke-dasharray="3,2"/>';
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="10" fill="'+color+'" />';
                            markerHtml += '<text x="'+pos.x+'" y="'+(pos.y+4)+'" font-size="11px" text-anchor="middle">'+emoji+'</text>';
                            markerHtml += '</g>';

                            layer.innerHTML += markerHtml;
                        });
                    }

                    // 4. RENDER EMERGENCY VEHICLES
                    if (currentCategory === "All" || currentCategory === "Emergency") {
                        specialVehicles.forEach(function(v) {
                            if (currentQuery !== "" && !v.name.toLowerCase().includes(currentQuery)) return;

                            var pos = latLngToXY(v.lat, v.lng);
                            if (v.id === "amb_1") { pos = { x: 442, y: 280 }; }
                            if (v.id === "fire_1") { pos = { x: 490, y: 440 }; }
                            if (v.id === "police_1") { pos = { x: 710, y: 360 }; }

                            var emoji = "🚑";
                            var color = "#EF4444";
                            if (v.type === "Fire") { emoji = "🚒"; color = "#F97316"; }
                            if (v.type === "Police") { emoji = "🚓"; color = "#3B82F6"; }

                            var markerHtml = '<g cursor="pointer" onclick="toast(\'' + v.name + ' tracking beacon.\')">';
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="15" fill="none" stroke="'+color+'" stroke-width="2"><animate attributeName="r" values="10;20;10" dur="2s" repeatCount="indefinite"/></circle>';
                            markerHtml += '<circle cx="'+pos.x+'" cy="'+pos.y+'" r="11" fill="'+color+'" style="filter: drop-shadow(0 0 6px '+color+');" />';
                            markerHtml += '<text x="'+pos.x+'" y="'+(pos.y+4.5)+'" font-size="11px" text-anchor="middle">'+emoji+'</text>';
                            markerHtml += '</g>';

                            layer.innerHTML += markerHtml;
                        });
                    }
                }

                // Digital Twin Active Sim-Vehicles Layer inside WebView
                var simulationInterval = null;
                
                function startTwinSimulation() {
                    stopTwinSimulation();
                    var layer = document.getElementById('markers-layer');
                    if (!layer) return;

                    var routePoints = [
                        { x: 280, y: 710 }, // Silk Board
                        { x: 440, y: 470 }, // Central Junction
                        { x: 410, y: 800 }, // Koramangala
                        { x: 735, y: 181 }  // Indiranagar
                    ];

                    var carState = { step: 0.0 };

                    simulationInterval = setInterval(function() {
                        carState.step += 0.05;
                        if (carState.step >= routePoints.length - 1) {
                            carState.step = 0;
                        }
                        var segmentIdx = Math.floor(carState.step);
                        var ratio = carState.step - segmentIdx;
                        var p1 = routePoints[segmentIdx];
                        var p2 = routePoints[segmentIdx + 1];

                        var currentX = p1.x + (p2.x - p1.x) * ratio;
                        var currentY = p1.y + (p2.y - p1.y) * ratio;

                        var existingCar = document.getElementById('sim-car-a');
                        if (existingCar) {
                            existingCar.setAttribute('cx', currentX);
                            existingCar.setAttribute('cy', currentY);
                            var pulse = document.getElementById('sim-car-pulse');
                            if (pulse) {
                                pulse.setAttribute('cx', currentX);
                                pulse.setAttribute('cy', currentY);
                            }
                        } else {
                            var simGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                            simGroup.setAttribute('id', 'sim-container');
                            simGroup.innerHTML = '<circle id="sim-car-pulse" cx="'+currentX+'" cy="'+currentY+'" r="14" fill="none" stroke="#A855F7" stroke-width="1.5"><animate attributeName="r" values="8;16;8" dur="1s" repeatCount="indefinite"/></circle><circle id="sim-car-a" cx="'+currentX+'" cy="'+currentY+'" r="8" fill="#A855F7" stroke="#FFFFFF" stroke-width="2"/>';
                            layer.appendChild(simGroup);
                        }
                    }, 120);
                }

                function stopTwinSimulation() {
                    if (simulationInterval) {
                        clearInterval(simulationInterval);
                        simulationInterval = null;
                    }
                    var sim = document.getElementById('sim-container');
                    if (sim) {
                        sim.parentNode.removeChild(sim);
                    }
                }

                // Initializer
                initMap();
                window.onload = function() {
                    initMap();
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isTablet = screenWidth >= 600.dp

        // Bottom Sheet State handles
        var sheetExpanded by remember { mutableStateOf(false) }
        var dragOffset by remember { mutableStateOf(0f) }

        val collapsedHeight = 180.dp
        val expandedHeight = 490.dp
        
        val animatedSheetHeight by animateDpAsState(
            targetValue = if (sheetExpanded) expandedHeight else collapsedHeight,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "SheetHeightAnimation"
        )

        // Parent layout container
        Box(modifier = Modifier.fillMaxSize()) {
            
            // MAP SECTION (Always fills the complete display in the background; prevents redrawing or missing content gaps)
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoaded = true
                                    view?.evaluateJavascript("setTimeout(function() { if (typeof map !== 'undefined' && map) { map.invalidateSize(); } }, 300);", null)
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                            
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onJunctionSelected(id: String) {
                                    post {
                                        viewModel.setSelectedJunction(id)
                                    }
                                }

                                @JavascriptInterface
                                fun toastMessage(msg: String) {
                                    post {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }, "AndroidBridge")

                            loadDataWithBaseURL("https://openstreetmap.org", commandCenterHTML, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )
            }

            // FLOATING SATELLITE COMMAND PANEL (Top Header + Search + Row Category Tabs)
            // Left margin on Wide screens creates Cupertino Left Sidebar, On small screen overlays completely.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = if (isTablet) 380.dp else 2000.dp)
                    .align(if (isTablet) Alignment.TopStart else Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Panel Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onNavigate("dashboard") },
                                modifier = Modifier
                                    .testTag("back_button")
                                    .size(36.dp)
                                    .background(BackgroundColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = SecondaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SATELLITE COMMAND",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryColor,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Smart Grid Observer",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SecondaryColor
                                )
                            }

                            // Live vs Twin Segments
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BackgroundColor)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (!isDigitalTwinMode) SecondaryColor else Color.Transparent)
                                        .clickable { 
                                            isDigitalTwinMode = false 
                                            activeLayerType = "hybrid"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Live",
                                        color = if (!isDigitalTwinMode) Color.White else SecondaryColor.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDigitalTwinMode) PrimaryColor else Color.Transparent)
                                        .clickable { 
                                            isDigitalTwinMode = true 
                                            activeLayerType = "digital_twin"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Troubleshoot,
                                            contentDescription = null,
                                            tint = if (isDigitalTwinMode) Color.White else SecondaryColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Twin",
                                            color = if (isDigitalTwinMode) Color.White else SecondaryColor.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Junction, Hospital, Signals...", fontSize = 12.sp, color = SecondaryColor.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryColor, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SecondaryColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("map_search_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryColor.copy(alpha = 0.5f),
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = BackgroundColor,
                                unfocusedContainerColor = BackgroundColor,
                                focusedTextColor = SecondaryColor,
                                unfocusedTextColor = SecondaryColor
                            ),
                            singleLine = true
                        )
                    }
                }

                // Horizontal Category selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        "All" to Icons.Default.AllInclusive,
                        "Signals" to Icons.Default.Traffic,
                        "Emergency" to Icons.Default.LocalHospital,
                        "Incidents" to Icons.Default.Warning,
                        "Hospitals" to Icons.Default.HomeWork,
                        "Traffic" to Icons.Default.Navigation,
                        "Weather" to Icons.Default.WbSunny,
                        "VIP Routes" to Icons.Default.Star
                    )

                    categories.forEach { (catName, icon) ->
                        val isSelected = selectedFilterCategory == catName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) SecondaryColor else Color.White.copy(alpha = 0.95f)
                                )
                                .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(16.dp))
                                .clickable { selectedFilterCategory = catName }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = catName,
                                    tint = if (isSelected) Color.White else SecondaryColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = catName,
                                    color = if (isSelected) Color.White else SecondaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // FLOATING MAP CONTROL DOCK (Map type, Zoom actions, GPS recalibration)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .padding(top = 150.dp), // lower to prevent header overlap
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Layer details selector
                Card(
                    modifier = Modifier
                        .width(44.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "standard" to Icons.Default.Map,
                            "satellite" to Icons.Default.Satellite,
                            "hybrid" to Icons.Default.Layers
                        ).forEach { (layer, icon) ->
                            val isSelected = activeLayerType == layer
                            IconButton(
                                onClick = { activeLayerType = layer },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SecondaryColor else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = layer,
                                    tint = if (isSelected) Color.White else SecondaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Mini Glass Zoom Engine (+ / -)
                Card(
                    modifier = Modifier
                        .width(44.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { webViewRef?.evaluateJavascript("zoomIn();", null) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = SecondaryColor, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = BorderColor, modifier = Modifier.width(28.dp))
                        IconButton(
                            onClick = { webViewRef?.evaluateJavascript("zoomOut();", null) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = SecondaryColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Current Location Tracker GPS Refresher
                FloatingActionButton(
                    onClick = {
                        if (!locationPermissionGranted) {
                            launcher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            systemGpsCoordinates?.let { (lat, lng) ->
                                webViewRef?.evaluateJavascript("centerOnJunction($lat, $lng);", null)
                            } ?: Toast.makeText(context, "Locking satellite GPS...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    containerColor = Color.White,
                    contentColor = if (locationPermissionGranted) PrimaryColor else SecondaryColor,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (locationPermissionGranted) Icons.Default.MyLocation else Icons.Default.LocationDisabled,
                        contentDescription = "Recenter GPS",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // RESPONSIVE CONTROL ANALYTICS: Side-panel on Tablets vs Draggable Bottom Sheet on Mobile
            if (isTablet) {
                // Tablet Sidebar Panel floats on the right of the map
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(360.dp)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 16.dp, end = 76.dp) // Offset slightly from edge controls
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "COMMAND OVERVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "City Analytics Core",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SecondaryColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render Active Junction details
                        ActiveJunctionPanel(
                            activeJunction = activeJunctionOnMap,
                            onNavigate = onNavigate,
                            viewModel = viewModel,
                            context = context
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Draggable equivalent full metrics index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AnalyticsMetricsDashboard(
                                isDigitalTwinMode = isDigitalTwinMode,
                                activeAlerts = activeAlerts
                            )
                        }
                    }
                }
            } else {
                // PHONE VIEW: DRAGGABLE INTERACTIVE BOTTOM SHEET DRAWER
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(animatedSheetHeight)
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset < -40f) {
                                        sheetExpanded = true
                                    } else if (dragOffset > 40f) {
                                        sheetExpanded = false
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = { dragOffset = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Touch-friendly dragging indicator bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sheetExpanded = !sheetExpanded }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 4.dp)
                                    .background(SecondaryColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            )
                        }

                        // Collapsed Quick Header Metrics
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "System Status",
                                        tint = PrimaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "COMMAND OBSERVATION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SecondaryColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Badge(
                                    containerColor = GreenState.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = "Traffic Index: 38% (Optimal)",
                                        color = GreenState,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Actionable alerts preview bar
                            if (activeAlerts.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFECEF))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "Alerts Active",
                                        tint = RedState,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ALERT: ${activeAlerts.first().title} - ${activeAlerts.first().description}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedState,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(GreenState, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "All grid loops operating within tolerable bandwidth bounds.",
                                        fontSize = 10.sp,
                                        color = SecondaryColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Divider(color = BorderColor, modifier = Modifier.padding(top = 12.dp))

                        // Scrollable metric suites visible when expanded or partially visible
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Selected Junction Inspector Card (Always visible inside bottom sheet)
                            ActiveJunctionPanel(
                                activeJunction = activeJunctionOnMap,
                                onNavigate = onNavigate,
                                viewModel = viewModel,
                                context = context
                            )

                            // Rest of the expanded metrics
                            AnimatedVisibility(
                                visible = sheetExpanded || isTablet,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                AnalyticsMetricsDashboard(
                                    isDigitalTwinMode = isDigitalTwinMode,
                                    activeAlerts = activeAlerts
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-component displaying details of the selected Active traffic junction and allowing optimization triggers
@Composable
fun ActiveJunctionPanel(
    activeJunction: com.example.data.JunctionEntity?,
    onNavigate: (String) -> Unit,
    viewModel: RYGViewModel,
    context: Context
) {
    if (activeJunction == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, BorderColor),
            colors = CardDefaults.cardColors(containerColor = BackgroundColor)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Select any signal node on map to inspect live controls.", fontSize = 12.sp, color = SecondaryColor.copy(alpha = 0.5f))
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, BorderColor),
            colors = CardDefaults.cardColors(containerColor = BackgroundColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top header label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when (activeJunction.currentPhase.uppercase()) {
                                        "GREEN" -> GreenState
                                        "RED" -> RedState
                                        else -> YellowState
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeJunction.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = SecondaryColor
                        )
                    }

                    Badge(
                        containerColor = when (activeJunction.congestionLevel) {
                            "Severe" -> RedState
                            "High" -> OrangeState
                            "Moderate" -> YellowState
                            else -> GreenState
                        }
                    ) {
                        Text(
                            text = activeJunction.congestionLevel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CURRENT SIGNAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryColor.copy(alpha = 0.5f))
                        Text("${activeJunction.currentPhase} (${activeJunction.timeRemaining}s)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SecondaryColor)
                    }
                    Column {
                        Text("ACTIVE LOAD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryColor.copy(alpha = 0.5f))
                        Text("${activeJunction.activeVehicles} vehicles", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryColor)
                    }
                    Column {
                        Text("DYNAMIC VELOCITY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryColor.copy(alpha = 0.5f))
                        val speedText = when (activeJunction.congestionLevel) {
                            "Severe" -> "11 km/h"
                            "High" -> "22 km/h"
                            "Moderate" -> "41 km/h"
                            else -> "56 km/h"
                        }
                        Text(speedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Button controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onNavigate("signal_control") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Optimizer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { 
                            val from = activeJunction.name
                            val to = "Victoria Emergency Hub"
                            viewModel.createEmergencyCorridor(from, to)
                            Toast.makeText(context, "EMS Preemption Corridor Set!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preempt Corridor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Expanded Dashboard View displaying metrics for collapsible panel
@Composable
fun AnalyticsMetricsDashboard(
    isDigitalTwinMode: Boolean,
    activeAlerts: List<com.example.data.AlertEntity>
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        
        // 1. Traffic Analytics Section
        Text(
            text = "DETAILED METRICS SYSTEM",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryColor,
            letterSpacing = 0.8.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ACTIVE LOOPS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3,485", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SecondaryColor)
                    Text("Connected IoT Devices", fontSize = 9.sp, color = SecondaryColor.copy(alpha = 0.5f))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("CITY DEVIATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("-12.4%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GreenState)
                    Text("vs historical average", fontSize = 9.sp, color = GreenState)
                }
            }
        }

        // 2. Emergency Status priority
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Emergency, contentDescription = null, tint = RedState, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Emergency Corridor Preemption", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryColor)
                    }

                    Badge(containerColor = RedState) {
                        Text("BLOCKED", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hebbal to MG Road Active Red corridor has green-wave preemption enabled. Dynamic signal phases will turn GREEN 12 seconds prior to EMS arrival.",
                    fontSize = 11.sp,
                    color = SecondaryColor.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = 0.75f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = RedState,
                    trackColor = BorderColor
                )
            }
        }

        // 3. AI TIMINGS RECOMMENDATIONS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Recommended Adjustments", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Model predicts 24% lower accumulation on Silk Board flyover by extending Phase 1 (Westbound Go) by 9 seconds. Optimal confidence matrix level: 97.4%.",
                    fontSize = 11.sp,
                    color = SecondaryColor.copy(alpha = 0.8f)
                )
            }
        }

        // 4. City Insights Tickers
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CITY GRID INSIGHTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryColor.copy(alpha = 0.5f))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(PurpleState, CircleShape))
                    Text("Digital twin environment is operating at 60 FPS sandbox simulation.", fontSize = 11.sp, color = SecondaryColor)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(PrimaryColor, CircleShape))
                    Text("Projected evening rush hour delay is reduced by 7 minutes due to rain clearing forecasts.", fontSize = 11.sp, color = SecondaryColor)
                }
            }
        }
    }
}
