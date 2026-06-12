package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RYGViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: RYGViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide Bottom Nav on splash and login screens
    val showBottomBar = currentRoute != "splash" && currentRoute != "login" && currentRoute != null

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                // Highly polished bottom navigation bar matching styling rules precisely
                NavigationBar(
                    containerColor = CardColor,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("app_bottom_nav")
                ) {
                    // Item 1: Home
                    val homeActive = currentRoute == "dashboard"
                    NavigationBarItem(
                        selected = homeActive,
                        onClick = { navController.navigate("dashboard") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryColor,
                            indicatorColor = PrimaryColor, // Active orange pill
                            unselectedTextColor = SecondaryColor.copy(alpha = 0.6f),
                            unselectedIconColor = SecondaryColor.copy(alpha = 0.6f)
                        )
                    )

                    // Item 2: Map
                    val mapActive = currentRoute == "live_traffic_map"
                    NavigationBarItem(
                        selected = mapActive,
                        onClick = { navController.navigate("live_traffic_map") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryColor,
                            indicatorColor = PrimaryColor,
                            unselectedTextColor = SecondaryColor.copy(alpha = 0.6f),
                            unselectedIconColor = SecondaryColor.copy(alpha = 0.6f)
                        )
                    )

                    // Item 3: Alerts
                    val alertsActive = currentRoute == "alerts"
                    NavigationBarItem(
                        selected = alertsActive,
                        onClick = { navController.navigate("alerts") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                        label = { Text("Alerts") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryColor,
                            indicatorColor = PrimaryColor,
                            unselectedTextColor = SecondaryColor.copy(alpha = 0.6f),
                            unselectedIconColor = SecondaryColor.copy(alpha = 0.6f)
                        )
                    )

                    // Item 4: Reports (Leads to Digital Twin or direct reports diagnostics)
                    val reportsActive = currentRoute == "reports" || currentRoute == "digital_twin"
                    NavigationBarItem(
                        selected = reportsActive,
                        onClick = { navController.navigate("digital_twin") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.BackupTable, contentDescription = "Digital Twin") },
                        label = { Text("Twin") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryColor,
                            indicatorColor = PrimaryColor,
                            unselectedTextColor = SecondaryColor.copy(alpha = 0.6f),
                            unselectedIconColor = SecondaryColor.copy(alpha = 0.6f)
                        )
                    )

                    // Item 5: Profile
                    val profileActive = currentRoute == "profile"
                    NavigationBarItem(
                        selected = profileActive,
                        onClick = { navController.navigate("profile") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryColor,
                            indicatorColor = PrimaryColor,
                            unselectedTextColor = SecondaryColor.copy(alpha = 0.6f),
                            unselectedIconColor = SecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Navigation Host binding our 12 unique user-experience states!
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(bottom = if (showBottomBar) 50.dp else 0.dp) // Offset padding from Navigation bar
        ) {
            composable("splash") {
                SplashScreen(viewModel = viewModel) { dest ->
                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            composable("login") {
                LoginScreen(viewModel = viewModel) { dest ->
                    navController.navigate(dest) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            composable("dashboard") {
                DashboardScreen(viewModel = viewModel) { target ->
                    viewModel.clearApiLogs()
                    navController.navigate(target)
                }
            }
            composable("traffic_monitor") {
                TrafficMonitorScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("live_traffic_map") {
                LiveTrafficMapScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("signal_control") {
                SignalControlScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("emergency_corridor") {
                EmergencyCorridorScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("ai_analytics") {
                AIAnalyticsScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("alerts") {
                AlertsScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("digital_twin") {
                DigitalTwinScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("reports") {
                ReportsScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
            composable("profile") {
                ProfileScreen(viewModel = viewModel) { target ->
                    navController.navigate(target)
                }
            }
        }
    }
}
