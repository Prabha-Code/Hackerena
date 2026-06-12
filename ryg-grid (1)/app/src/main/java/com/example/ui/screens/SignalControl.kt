package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel
import kotlinx.coroutines.delay

@Composable
fun SignalControlScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val junctions by viewModel.junctions.collectAsState()
    val selectedJunctionId by viewModel.selectedJunctionId.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val apiResultLog by viewModel.apiResultLog.collectAsState()

    val optGreen by viewModel.aiOptimizedGreen.collectAsState()
    val optYellow by viewModel.aiOptimizedYellow.collectAsState()
    val optRed by viewModel.aiOptimizedRed.collectAsState()

    val currentJunction = remember(junctions, selectedJunctionId) {
        junctions.find { it.id == selectedJunctionId } ?: junctions.firstOrNull()
    }

    // Active ticks ticking simulation
    var tickerTime by remember(currentJunction) {
        mutableStateOf(currentJunction?.timeRemaining ?: 18)
    }
    var tickerPhases by remember(currentJunction) {
        mutableStateOf(currentJunction?.currentPhase ?: "Green")
    }

    LaunchedEffect(currentJunction) {
        while (true) {
            delay(1000)
            if (tickerTime > 1) {
                tickerTime--
            } else {
                // cycle phase
                when (tickerPhases.lowercase()) {
                    "green" -> {
                        tickerPhases = "Yellow"
                        tickerTime = 5
                    }
                    "yellow" -> {
                        tickerPhases = "Red"
                        tickerTime = 30
                    }
                    else -> {
                        tickerPhases = "Green"
                        tickerTime = currentJunction?.timeRemaining ?: 18
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        val isTablet = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                    text = "Signal Control",
                    color = SecondaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentJunction != null) {
                // JUNCTION DETAIL BANNER (Common top bar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentJunction.name,
                            color = SecondaryColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Current Congestion: ${currentJunction.congestionLevel}",
                            color = SecondaryColor.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Online indicator badge
                    Row(
                        modifier = Modifier
                            .background(GreenState.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(GreenState, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Online",
                            color = GreenState,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isTablet) {
                    // RESPONSIVE TWO-COLUMN LAYOUT FOR TABLETS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // LEFT SIDECOLUMN: 3D Signal & Active Phase info
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(24.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 3D Visual Traffic Light
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(240.dp)
                                            .background(SecondaryColor, RoundedCornerShape(32.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.SpaceEvenly,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            // RED LIGHT
                                            val isRed = tickerPhases.lowercase() == "red"
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .shadow(if (isRed) 16.dp else 0.dp, CircleShape)
                                                    .background(
                                                        if (isRed) RedState else RedState.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .border(2.dp, if (isRed) Color.White else Color.Transparent, CircleShape)
                                            )

                                            // YELLOW LIGHT
                                            val isYellow = tickerPhases.lowercase() == "yellow"
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .shadow(if (isYellow) 16.dp else 0.dp, CircleShape)
                                                    .background(
                                                        if (isYellow) YellowState else YellowState.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .border(2.dp, if (isYellow) Color.White else Color.Transparent, CircleShape)
                                            )

                                            // GREEN LIGHT
                                            val isGreen = tickerPhases.lowercase() == "green"
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .shadow(if (isGreen) 16.dp else 0.dp, CircleShape)
                                                    .background(
                                                        if (isGreen) GreenState else GreenState.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .border(2.dp, if (isGreen) Color.White else Color.Transparent, CircleShape)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Active Phase Metadata Info
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Current Phase",
                                            fontSize = 12.sp,
                                            color = SecondaryColor.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = tickerPhases,
                                            fontSize = 24.sp,
                                            color = when(tickerPhases.lowercase()) {
                                                "red" -> RedState
                                                "yellow" -> YellowState
                                                else -> GreenState
                                            },
                                            fontWeight = FontWeight.Black
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Time Remaining",
                                            fontSize = 12.sp,
                                            color = SecondaryColor.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = String.format("%02d", tickerTime),
                                                fontSize = 52.sp,
                                                color = when(tickerPhases.lowercase()) {
                                                    "red" -> RedState
                                                    "yellow" -> YellowState
                                                    else -> GreenState
                                                },
                                                fontWeight = FontWeight.Light,
                                                lineHeight = 52.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "sec",
                                                fontSize = 14.sp,
                                                color = SecondaryColor.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Next Phase",
                                            fontSize = 11.sp,
                                            color = SecondaryColor.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when(tickerPhases.lowercase()) {
                                                "green" -> "Yellow 05s"
                                                "yellow" -> "Red 30s"
                                                else -> "Green ${currentJunction.timeRemaining}s"
                                            },
                                            fontSize = 13.sp,
                                            color = SecondaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // RIGHT SIDECOLUMN: AI Timings and Optimization
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(24.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Optimized Timing Set",
                                        color = SecondaryColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TimingPill(label = "Green", seconds = optGreen, color = GreenState)
                                        TimingPill(label = "Yellow", seconds = optYellow, color = YellowState)
                                        TimingPill(label = "Red", seconds = optRed, color = RedState)
                                    }
                                }
                            }

                            // OPTIMIZE NOW BUTTON
                            Button(
                                onClick = { viewModel.runAIOptimize(currentJunction.id) },
                                enabled = !isAnalyzing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("optimize_now_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Optimize Now ✨",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Signal Optimizer Diagnosis
                            if (apiResultLog != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = CardColor)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Signal Optimizer Explanation",
                                            color = SecondaryColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = apiResultLog!!,
                                            color = SecondaryColor.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // PHONE VIEW: Draggable standard column flow
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Main Visual Traffic Light
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Sleek 3D Traffic Light Casing
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(260.dp)
                                    .shadow(12.dp, RoundedCornerShape(40.dp))
                                    .background(SecondaryColor, RoundedCornerShape(40.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    // RED LIGHT
                                    val isRed = tickerPhases.lowercase() == "red"
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .shadow(if (isRed) 16.dp else 0.dp, CircleShape)
                                            .background(
                                                if (isRed) RedState else RedState.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .border(2.dp, if (isRed) Color.White else Color.Transparent, CircleShape)
                                    )

                                    // YELLOW LIGHT
                                    val isYellow = tickerPhases.lowercase() == "yellow"
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .shadow(if (isYellow) 16.dp else 0.dp, CircleShape)
                                            .background(
                                                if (isYellow) YellowState else YellowState.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .border(2.dp, if (isYellow) Color.White else Color.Transparent, CircleShape)
                                    )

                                    // GREEN LIGHT
                                    val isGreen = tickerPhases.lowercase() == "green"
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .shadow(if (isGreen) 16.dp else 0.dp, CircleShape)
                                            .background(
                                                if (isGreen) GreenState else GreenState.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .border(2.dp, if (isGreen) Color.White else Color.Transparent, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // 2. Active Phase Metadata block
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Current Phase",
                                    fontSize = 13.sp,
                                    color = SecondaryColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tickerPhases,
                                    fontSize = 28.sp,
                                    color = when(tickerPhases.lowercase()) {
                                        "red" -> RedState
                                        "yellow" -> YellowState
                                        else -> GreenState
                                    },
                                    fontWeight = FontWeight.Black
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Time Remaining",
                                    fontSize = 13.sp,
                                    color = SecondaryColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format("%02d", tickerTime),
                                        fontSize = 64.sp,
                                        color = when(tickerPhases.lowercase()) {
                                            "red" -> RedState
                                            "yellow" -> YellowState
                                            else -> GreenState
                                        },
                                        fontWeight = FontWeight.Light,
                                        lineHeight = 64.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "sec",
                                        fontSize = 16.sp,
                                        color = SecondaryColor.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Next Phase",
                                    fontSize = 12.sp,
                                    color = SecondaryColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when(tickerPhases.lowercase()) {
                                        "green" -> "Yellow 05s"
                                        "yellow" -> "Red 30s"
                                        else -> "Green ${currentJunction.timeRemaining}s"
                                    },
                                    fontSize = 14.sp,
                                    color = SecondaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // AI OPTIMIZED TIMING SET CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(24.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardColor)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "AI Optimized Timing Set",
                                    color = SecondaryColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TimingPill(label = "Green", seconds = optGreen, color = GreenState)
                                    TimingPill(label = "Yellow", seconds = optYellow, color = YellowState)
                                    TimingPill(label = "Red", seconds = optRed, color = RedState)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ACTION BUTTON: OPTIMIZE NOW ✨
                        Button(
                            onClick = { viewModel.runAIOptimize(currentJunction.id) },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("optimize_now_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Optimize Now ✨",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Signal Optimizer Explanation
                        if (apiResultLog != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Signal Optimizer Explanation",
                                        color = SecondaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = apiResultLog!!,
                                        color = SecondaryColor.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimingPill(
    label: String,
    seconds: Int,
    color: Color
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${seconds} sec",
            color = SecondaryColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
