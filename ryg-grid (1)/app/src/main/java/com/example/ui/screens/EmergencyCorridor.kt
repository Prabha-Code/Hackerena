package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel

@Composable
fun EmergencyCorridorScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val corridors by viewModel.corridors.collectAsState()
    
    // Inputs for requesting new green corridors
    var fromHospital by remember { mutableStateOf("St. John's Hospital") }
    var toHospital by remember { mutableStateOf("Fortis Hospital BG Road") }

    val activeCorridor = remember(corridors) {
        corridors.firstOrNull { it.isActive } ?: corridors.firstOrNull()
    }

    // Animation progress for live tracking ambulance moving
    val infiniteTransition = rememberInfiniteTransition()
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

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
                text = "Emergency Corridor",
                color = SecondaryColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeCorridor != null) {
            // CORRIDOR STATS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Corridor",
                                color = SecondaryColor.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeCorridor.id,
                                color = SecondaryColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Pulser green active badge
                        Row(
                            modifier = Modifier
                                .background(GreenState.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = "Medical",
                                tint = GreenState,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active",
                                color = GreenState,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From", color = SecondaryColor.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(activeCorridor.fromLocation, color = SecondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To", color = SecondaryColor.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(activeCorridor.toLocation, color = SecondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Distance", color = SecondaryColor.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${activeCorridor.distanceKm} km", color = SecondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ETA", color = SecondaryColor.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(String.format("%02d min", activeCorridor.etaMin), color = AccentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // VECTOR TRACKER MAP CANVAS (Shows route progress rendering!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw start and end pin locations with connector road
                    val startOffset = Offset(30.dp.toPx(), h * 0.7f)
                    val controlOffset = Offset(w * 0.4f, h * 0.2f)
                    val endOffset = Offset(w - 30.dp.toPx(), h * 0.4f)
                    
                    val path = Path().apply {
                        moveTo(startOffset.x, startOffset.y)
                        quadraticTo(controlOffset.x, controlOffset.y, endOffset.x, endOffset.y)
                    }

                    // Draw base roadway
                    drawPath(
                        path = path,
                        color = SecondaryColor.copy(alpha = 0.08f),
                        style = Stroke(width = 10.dp.toPx())
                    )
                    
                    // Draw glowing green cleared road
                    drawPath(
                        path = path,
                        color = GreenState,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw start Pin (Green)
                    drawCircle(GreenState, 8.dp.toPx(), startOffset)
                    drawCircle(Color.White, 3.dp.toPx(), startOffset)

                    // Draw end Pin (Red)
                    drawCircle(AccentColor, 8.dp.toPx(), endOffset)
                    drawCircle(Color.White, 3.dp.toPx(), endOffset)

                    // Compute current position of moving ambulance dot matching anim
                    val t = animatedProgress
                    val px = (1-t)*(1-t)*startOffset.x + 2*(1-t)*t*controlOffset.x + t*t*endOffset.x
                    val py = (1-t)*(1-t)*startOffset.y + 2*(1-t)*t*controlOffset.y + t*t*endOffset.y
                    
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = GreenState,
                        radius = 6.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PULSING LIVE TRACKING BUTTON Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentColor, RoundedCornerShape(16.dp))
                    .clickable { /* Toggle trigger tracking */ }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Live Signal Tracker",
                        tint = AccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(( • )) Live Tracking",
                        color = AccentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // TRIGGER GREEN WAVE PANEL (Form submission adds corridor to database)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Request Emergency Green Wave",
                    color = SecondaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Autonomously clears all signal lights to green ahead of the route in cascade.",
                    color = SecondaryColor.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fromHospital,
                    onValueChange = { fromHospital = it },
                    label = { Text("Starting dispatch location") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "From", tint = GreenState) },
                    modifier = Modifier.fillMaxWidth().testTag("dispatch_from_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = toHospital,
                    onValueChange = { toHospital = it },
                    label = { Text("Destination hospital") },
                    leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = "To", tint = AccentColor) },
                    modifier = Modifier.fillMaxWidth().testTag("dispatch_to_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.createEmergencyCorridor(fromHospital, toHospital)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_corridor_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenState)
                ) {
                    Text(
                        text = "Dispatch Ambulance & Clear Signals",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // padding space
    }
}
