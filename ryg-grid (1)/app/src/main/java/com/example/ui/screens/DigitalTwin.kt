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
import androidx.compose.ui.geometry.Size
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
fun DigitalTwinScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val junctions by viewModel.junctions.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val activeSimulationOutput by viewModel.activeSimulationOutput.collectAsState()

    var selectedScenario by remember { mutableStateOf("Accident") }

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
                text = "Digital Twin Simulator",
                color = SecondaryColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CITY ISOMETRIC MODEL DRAWER CANVAS CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E2E3A))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
        ) {
            // Draw an actual simulated isometric city matrix grid projection
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Draw 3D grid plane lines
                val cols = 8
                val spacing = w / cols
                
                for (i in -4..cols+4) {
                    // Isometric projection diagonals
                    drawLine(
                        color = Color(0x2210B981), // glowing grid
                        start = Offset(i * spacing, 0f),
                        end = Offset((i - 4) * spacing, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x2210B981),
                        start = Offset(i * spacing, 0f),
                        end = Offset((i + 4) * spacing, h),
                        strokeWidth = 1f
                    )
                }

                // Draw solid isometric wireframe buildings blocks
                val houseWidth = 35.dp.toPx()
                val centerOffset = Offset(w * 0.45f, h * 0.5f)
                
                val buildingPath = Path().apply {
                    // Top rhombus
                    moveTo(centerOffset.x, centerOffset.y - 40.dp.toPx())
                    lineTo(centerOffset.x + houseWidth, centerOffset.y - 20.dp.toPx())
                    lineTo(centerOffset.x, centerOffset.y)
                    lineTo(centerOffset.x - houseWidth, centerOffset.y - 20.dp.toPx())
                    close()
                    // Sides extrusion down
                    moveTo(centerOffset.x - houseWidth, centerOffset.y - 20.dp.toPx())
                    lineTo(centerOffset.x - houseWidth, centerOffset.y + 30.dp.toPx())
                    lineTo(centerOffset.x, centerOffset.y + 50.dp.toPx())
                    lineTo(centerOffset.x + houseWidth, centerOffset.y + 30.dp.toPx())
                    lineTo(centerOffset.x + houseWidth, centerOffset.y - 20.dp.toPx())
                }
                drawPath(buildingPath, Color(0xFF233D4C).copy(alpha = 0.85f))
                drawPath(buildingPath, Color(0xFF10B981), style = Stroke(width = 1.dp.toPx()))

                // Other smaller block
                val secondaryCenter = Offset(w * 0.75f, h * 0.35f)
                val buildingPath2 = Path().apply {
                    moveTo(secondaryCenter.x, secondaryCenter.y - 25.dp.toPx())
                    lineTo(secondaryCenter.x + 20.dp.toPx(), secondaryCenter.y - 12.dp.toPx())
                    lineTo(secondaryCenter.x, secondaryCenter.y)
                    lineTo(secondaryCenter.x - 20.dp.toPx(), secondaryCenter.y - 12.dp.toPx())
                    close()
                    moveTo(secondaryCenter.x - 20.dp.toPx(), secondaryCenter.y - 12.dp.toPx())
                    lineTo(secondaryCenter.x - 20.dp.toPx(), secondaryCenter.y + 20.dp.toPx())
                    lineTo(secondaryCenter.x, secondaryCenter.y + 32.dp.toPx())
                    lineTo(secondaryCenter.x + 20.dp.toPx(), secondaryCenter.y + 20.dp.toPx())
                    lineTo(secondaryCenter.x + 20.dp.toPx(), secondaryCenter.y - 12.dp.toPx())
                }
                drawPath(buildingPath2, Color(0xFF233D4C).copy(alpha = 0.85f))
                drawPath(buildingPath2, Color(0xFF10B981), style = Stroke(width = 1.dp.toPx()))

                // Draw a pulsing overlay highlighting scenario impact
                drawCircle(
                    color = Color.Red.copy(alpha = 0.25f),
                    radius = 35.dp.toPx(),
                    center = Offset(w * 0.35f, h * 0.72f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(w * 0.35f, h * 0.72f)
                )
            }

            // Gimmick details overlay
            Text(
                text = "Live Digital Twin Mesh Projection",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SIMULATION SCENARIO GRID LIST SELECTOR (Accident, Road Closure, Weather, VIP)
        Text(
            text = "Select Simulation Scenarios",
            color = SecondaryColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select an event to run dynamic AI model forecasts on city block latency.",
            color = SecondaryColor.copy(alpha = 0.5f),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3x2 Grid items list (Matches layout perfectly)
        val scenariosList = listOf(
            ScenarioItem("Accident", Icons.Default.Warning, RedState),
            ScenarioItem("Road Closure", Icons.Default.Block, OrangeState),
            ScenarioItem("Construction", Icons.Default.Construction, YellowState),
            ScenarioItem("Signal Failure", Icons.Default.Traffic, GreenState),
            ScenarioItem("VIP Movement", Icons.Default.SupervisorAccount, PurpleState),
            ScenarioItem("Weather Impact", Icons.Default.Cloud, BlueState)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            for (i in scenariosList.indices step 2) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val s1 = scenariosList[i]
                    val s2 = scenariosList[i+1]

                    ScenarioCard(
                        scenario = s1,
                        isSelected = selectedScenario == s1.name,
                        modifier = Modifier.weight(1f)
                    ) { selectedScenario = s1.name }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    ScenarioCard(
                        scenario = s2,
                        isSelected = selectedScenario == s2.name,
                        modifier = Modifier.weight(1f)
                    ) { selectedScenario = s2.name }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // RUN SIMULATION BUTTON (Triggers real Gemini simulator)
        Button(
            onClick = { viewModel.runDigitalTwinSimulation(selectedScenario, "silk_board") },
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_simulation_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Run Simulation",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Real AI simulation output forecast reporting!
        if (activeSimulationOutput != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Digital Twin Forecasting Diagnosis",
                        color = SecondaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeSimulationOutput!!,
                        color = SecondaryColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // padding space
    }
}

@Composable
fun ScenarioCard(
    scenario: ScenarioItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                2.dp,
                if (isSelected) scenario.accentColor else BorderColor,
                RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .shadow(if (isSelected) 4.dp else 1.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) scenario.accentColor.copy(alpha = 0.05f) else CardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(scenario.accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = scenario.icon,
                    contentDescription = scenario.name,
                    tint = scenario.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = scenario.name,
                color = SecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class ScenarioItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)
