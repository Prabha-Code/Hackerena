package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentColor
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardColor
import com.example.ui.theme.GreenState
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.SecondaryColor
import com.example.ui.viewmodel.RYGViewModel

@Composable
fun DashboardScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val junctions by viewModel.junctions.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val corridors by viewModel.corridors.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val mysqlStatus by viewModel.mysqlStatus.collectAsState()
    val mysqlIsSyncing by viewModel.mysqlIsSyncing.collectAsState()

    val totalVehicles = remember(junctions) {
        junctions.sumOf { it.activeVehicles }
    }
    
    val criticalAlertsCount = remember(alerts) {
        alerts.count { it.type == "CRITICAL" }
    }

    val activeCorridorsCount = remember(corridors) {
        corridors.count { it.isActive }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { /* Menu */ },
                    modifier = Modifier.testTag("menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = SecondaryColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dashboard",
                    color = SecondaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box {
                IconButton(
                    onClick = { onNavigate("alerts") },
                    modifier = Modifier.testTag("notification_bell")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = SecondaryColor
                    )
                }
                if (criticalAlertsCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(AccentColor, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = (4).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = criticalAlertsCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Good Morning,",
            color = SecondaryColor.copy(alpha = 0.6f),
            fontSize = 15.sp
        )
        Text(
            text = "$userRole 👋",
            color = SecondaryColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // OVERALL TRAFFIC INDEX CARD (Premium gradient + sparkline canvas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AccentColor, PrimaryColor)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Overall Traffic Index",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "72%",
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "High Congestion",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // SPARKLINE GRAPH DRAWER
                    Canvas(
                        modifier = Modifier
                            .width(140.dp)
                            .height(50.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.8f)
                            quadraticTo(
                                size.width * 0.25f, size.height * 0.4f,
                                size.width * 0.5f, size.height * 0.65f
                            )
                            quadraticTo(
                                size.width * 0.75f, size.height * 0.2f,
                                size.width, size.height * 0.1f
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(size.width, size.height * 0.1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Trend up",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "12% from yesterday",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // METRICS GRID (2x2 cards)
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Junctions",
                value = junctions.size.toString(),
                status = "Active",
                statusColor = GreenState,
                icon = Icons.Default.Traffic,
                iconTint = AccentColor,
                modifier = Modifier.weight(1f)
            ) { onNavigate("live_traffic_map") }
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Live Vehicles",
                value = String.format("%,d", if(totalVehicles > 0) totalVehicles else 12845),
                status = "On Roads",
                statusColor = PrimaryColor,
                icon = Icons.Default.DirectionsCar,
                iconTint = PrimaryColor,
                modifier = Modifier.weight(1f)
            ) { onNavigate("traffic_monitor") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Active Alerts",
                value = String.format("%02d", alerts.size),
                status = "Critical",
                statusColor = AccentColor,
                icon = Icons.Default.Warning,
                iconTint = AccentColor,
                modifier = Modifier.weight(1f)
            ) { onNavigate("alerts") }
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Emerg. Corridors",
                value = String.format("%02d", activeCorridorsCount),
                status = "Active",
                statusColor = GreenState,
                icon = Icons.Default.LocalHospital,
                iconTint = GreenState,
                modifier = Modifier.weight(1f)
            ) { onNavigate("emergency_corridor") }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AIVEN CLOUD DATABASE MANAGEMENT PANEL
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .testTag("mysql_sync_panel"),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Aiven MySQL Cloud",
                            tint = PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aiven MySQL Cloud",
                            color = SecondaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = mysqlStatus,
                            color = if (mysqlStatus.contains("Connected") || mysqlStatus.contains("Complete")) GreenState else SecondaryColor.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (mysqlIsSyncing) {
                        CircularProgressIndicator(
                            color = PrimaryColor,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                if (!mysqlIsSyncing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testMySQLConnection() },
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = "Test connection",
                                tint = SecondaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test", color = SecondaryColor, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.syncWithMySQLRemote() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync database",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QUICK ACTIONS
        Text(
            text = "Quick Actions",
            color = SecondaryColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionItem(
                label = "Traffic\nMonitor",
                icon = Icons.Default.Visibility,
                tintColor = PrimaryColor
            ) { onNavigate("traffic_monitor") }
            QuickActionItem(
                label = "Signal\nControl",
                icon = Icons.Default.Traffic,
                tintColor = AccentColor
            ) {
                viewModel.setSelectedJunction("silk_board")
                onNavigate("signal_control")
            }
            QuickActionItem(
                label = "Emergency\nCorridor",
                icon = Icons.Default.Speed,
                tintColor = GreenState
            ) { onNavigate("emergency_corridor") }
            QuickActionItem(
                label = "AI\nAnalytics",
                icon = Icons.Default.Assessment,
                tintColor = SecondaryColor
            ) { onNavigate("ai_analytics") }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // padding space for bottom bar
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                color = SecondaryColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = SecondaryColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = status,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun QuickActionItem(
    label: String,
    icon: ImageVector,
    tintColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .background(CardColor, RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = SecondaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall,
            softWrap = true,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
