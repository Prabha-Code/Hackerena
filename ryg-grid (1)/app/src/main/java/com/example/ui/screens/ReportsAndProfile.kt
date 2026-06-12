package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel

@Composable
fun ReportsScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate("dashboard") },
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SecondaryColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reports & Diagnostics",
                color = SecondaryColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CARDS LISTING REAL SYSTEMS INDICATORS (Efficiency logs)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Weekly Efficiency Summary",
                    color = SecondaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Key performance statistics on green light coordination.",
                    color = SecondaryColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                EfficiencyMetricItem(label = "Signal wave delay reduced", value = "24.3%", iconColor = GreenState)
                Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
                EfficiencyMetricItem(label = "Ambulance ETA saved avg", value = "03:45 min", iconColor = AccentColor)
                Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
                EfficiencyMetricItem(label = "Adaptive green loop utilization", value = "92.8%", iconColor = PrimaryColor)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RECENT SIMULATION LOGS
        Text(
            text = "AI System Diagnostic Notes",
            color = SecondaryColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Operator Logs: Real-Time Wave Loops running adaptively. Camera node [Silk Board #01] reporting normal image stream count sync. Overall database health green with zero signal packet losses.",
                    color = SecondaryColor,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Printable format trigger button
        Button(
            onClick = { /* Download files mock feedback */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("download_report_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = "Download")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compile Printable PDF Diagnostic", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // padding space
    }
}

@Composable
fun EfficiencyMetricItem(
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(iconColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = SecondaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = value,
            color = SecondaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun ProfileScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val username by viewModel.username.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate("dashboard") },
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SecondaryColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Console Settings",
                color = SecondaryColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PROFILE CARD AVATAR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large styled user icon inside circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(PrimaryColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User",
                        tint = PrimaryColor,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = username,
                    color = SecondaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "System Operator: $userRole",
                    color = SecondaryColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Build indicator
                Row(
                    modifier = Modifier
                        .background(SecondaryColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Build v1.20-Prod",
                        color = SecondaryColor.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MANAGE ROLES SELECTOR CAPSULE LIST DIRECTLY CHANGING CONSOLE ROLE
        Text(
            text = "Swap Console Clearance Clearances",
            color = SecondaryColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val roles = listOf("Traffic Officer", "Admin", "Citizen")
                roles.forEach { role ->
                    val isSelected = userRole == role
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setUserRole(role) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = role,
                            color = SecondaryColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setUserRole(role) },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // LOG OUT BUTTON returning user directly to login screen!
        Button(
            onClick = {
                viewModel.setLoggedIn(false)
                onNavigate("login")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Logout, contentDescription = "Log out", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Disconnect Console Sign-Out", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // padding space
    }
}
