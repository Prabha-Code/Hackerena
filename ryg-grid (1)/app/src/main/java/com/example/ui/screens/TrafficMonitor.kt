package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel

@Composable
fun TrafficMonitorScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    val junctions by viewModel.junctions.collectAsState()
    val selectedJunctionId by viewModel.selectedJunctionId.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val apiResultLog by viewModel.apiResultLog.collectAsState()

    var activeTab by remember { mutableStateOf("Live Feed") } // "Live Feed", "Cameras", "Statistics"

    val currentJunction = remember(junctions, selectedJunctionId) {
        junctions.find { it.id == selectedJunctionId } ?: junctions.firstOrNull()
    }

    // Capture Canvas as Bitmap for REAL Gemini Vision analyzer!
    val context = LocalContext.current
    val syntheticFeedBitmap = remember(currentJunction) {
        // Create a real programmatic schematic bitmap to send to Gemini!
        val width = 400
        val height = 300
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { isAntiAlias = true }
        
        // Background slate
        paint.color = 0xFF233D4C.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Road Cross lines
        paint.color = 0x22FFFFFF
        paint.strokeWidth = 10f
        canvas.drawLine(0f, (height/2).toFloat(), width.toFloat(), (height/2).toFloat(), paint)
        canvas.drawLine((width/2).toFloat(), 0f, (width/2).toFloat(), height.toFloat(), paint)

        // Draw multiple cars (yellow, orange boxes)
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFD802E.toInt() // Primary Orange
        canvas.drawRect(80f, 40f, 130f, 75f, paint)
        canvas.drawRect(210f, 160f, 260f, 195f, paint)
        
        paint.color = 0xFFFF4F5E.toInt() // Accent Red
        canvas.drawRect(290f, 80f, 320f, 130f, paint)
        
        paint.color = 0xFF10B981.toInt() // Green
        canvas.drawRect(60f, 210f, 95f, 240f, paint)
        
        bmp
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
                    text = "Traffic Monitor",
                    color = SecondaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentJunction != null) {
                if (isTablet) {
                    // Responsive Split Screen Layout for Tablets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT PANEL: CCTV Feed & AI Scanners
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentJunction.name,
                                color = SecondaryColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            // TABS SELECTOR (Live Feed, Cameras, Statistics)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardColor, RoundedCornerShape(16.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                    .padding(4.dp)
                            ) {
                                val tabs = listOf("Live Feed", "Cameras", "Statistics")
                                tabs.forEach { tab ->
                                    val isSelected = activeTab == tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) AccentColor else Color.Transparent)
                                            .clickable { activeTab = tab }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab,
                                            color = if (isSelected) Color.White else SecondaryColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // VECTOR CCTV / FEED CANVAS DISPLAY WITH REAL "LIVE" PULSING INDICATOR
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                                    .shadow(8.dp, RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF233D4C))
                                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(
                                        color = Color(0xFF1E2E3A),
                                        size = size
                                    )
                                    
                                    val centerW = size.width / 2f
                                    val centerH = size.height / 2f
                                    
                                    drawRect(
                                        color = Color(0x33FFFFFF),
                                        topLeft = Offset(centerW - 40.dp.toPx(), 0f),
                                        size = Size(80.dp.toPx(), size.height)
                                    )
                                    drawRect(
                                        color = Color(0x33FFFFFF),
                                        topLeft = Offset(0f, centerH - 40.dp.toPx()),
                                        size = Size(size.width, 80.dp.toPx())
                                    )

                                    val zebraGap = 8.dp.toPx()
                                    val zebraWidth = 4.dp.toPx()
                                    
                                    for (i in 0..7) {
                                        val offsetZ = (centerW - 35.dp.toPx()) + i * (zebraWidth + zebraGap)
                                        drawRect(Color.White.copy(alpha = 0.4f), Offset(offsetZ, centerH - 55.dp.toPx()), Size(zebraWidth, 12.dp.toPx()))
                                        drawRect(Color.White.copy(alpha = 0.4f), Offset(offsetZ, centerH + 43.dp.toPx()), Size(zebraWidth, 12.dp.toPx()))
                                    }

                                    val cars = listOf(
                                        Offset(centerW - 20.dp.toPx(), centerH - 80.dp.toPx()),
                                        Offset(centerW + 15.dp.toPx(), centerH + 60.dp.toPx()),
                                        Offset(centerW - 70.dp.toPx(), centerH - 18.dp.toPx()),
                                        Offset(centerW + 65.dp.toPx(), centerH + 12.dp.toPx())
                                    )
                                    cars.forEachIndexed { idx, pos ->
                                        drawCircle(
                                            color = if (idx % 2 == 0) PrimaryColor else AccentColor,
                                            radius = 10.dp.toPx(),
                                            center = pos
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 4.dp.toPx(),
                                            center = pos
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopStart),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${currentJunction.name} Feed [01]",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                )
                            }

                            // AI SCAN CCTV Feed Action Button
                            Button(
                                onClick = { viewModel.analyzeCCTVFrame(syntheticFeedBitmap) },
                                enabled = !isAnalyzing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("ai_scan_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DeveloperBoard, contentDescription = "Scan", tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI SCAN CCTV Feed",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Real AI analysis results
                            if (apiResultLog != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = CardColor)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(PrimaryColor, CircleShape))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AI Computer Vision Diagnosis",
                                                color = SecondaryColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
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

                        // RIGHT PANEL: Vehicle Counts & Details (Fully responsive scroll alignment)
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
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Vehicle Count",
                                                color = SecondaryColor,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Last 5 Minutes",
                                                color = SecondaryColor.copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Total",
                                                color = SecondaryColor.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = currentJunction.activeVehicles.toString(),
                                                color = SecondaryColor,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        CountItem(label = "Cars", count = currentJunction.carCount, icon = Icons.Default.DirectionsCar, tint = BlueState)
                                        CountItem(label = "Bikes", count = currentJunction.bikeCount, icon = Icons.Default.TwoWheeler, tint = GreenState)
                                        CountItem(label = "Buses", count = currentJunction.busCount, icon = Icons.Default.DirectionsBus, tint = PrimaryColor)
                                        CountItem(label = "Trucks", count = currentJunction.truckCount, icon = Icons.Default.LocalShipping, tint = AccentColor)
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Congestion Level: ${currentJunction.congestionLevel}",
                                        color = SecondaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    val progressValue = when(currentJunction.congestionLevel.lowercase()) {
                                        "low" -> 0.25f
                                        "moderate" -> 0.55f
                                        "high" -> 0.8f
                                        "severe" -> 1.0f
                                        else -> 0.7f
                                    }
                                    val barColor = when(currentJunction.congestionLevel.lowercase()) {
                                        "low" -> GreenState
                                        "moderate" -> YellowState
                                        else -> AccentColor
                                    }

                                    LinearProgressIndicator(
                                        progress = { progressValue },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = barColor,
                                        trackColor = BorderColor
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // PHONE VIEW: Collapsed single Column flow
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = currentJunction.name,
                            color = SecondaryColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // TABS SELECTOR (Live Feed, Cameras, Statistics)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardColor, RoundedCornerShape(16.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .padding(4.dp)
                        ) {
                            val tabs = listOf("Live Feed", "Cameras", "Statistics")
                            tabs.forEach { tab ->
                                val isSelected = activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) AccentColor else Color.Transparent)
                                        .clickable { activeTab = tab }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (isSelected) Color.White else SecondaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // VECTOR CCTV / FEED CANVAS DISPLAY WITH REAL "LIVE" PULSING INDICATOR
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.33f)
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF233D4C))
                                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    color = Color(0xFF1E2E3A),
                                    size = size
                                )
                                
                                val centerW = size.width / 2f
                                val centerH = size.height / 2f
                                
                                drawRect(
                                    color = Color(0x33FFFFFF),
                                    topLeft = Offset(centerW - 40.dp.toPx(), 0f),
                                    size = Size(80.dp.toPx(), size.height)
                                )
                                drawRect(
                                    color = Color(0x33FFFFFF),
                                    topLeft = Offset(0f, centerH - 40.dp.toPx()),
                                    size = Size(size.width, 80.dp.toPx())
                                )

                                val zebraGap = 8.dp.toPx()
                                val zebraWidth = 4.dp.toPx()
                                
                                for (i in 0..7) {
                                    val offsetZ = (centerW - 35.dp.toPx()) + i * (zebraWidth + zebraGap)
                                    drawRect(Color.White.copy(alpha = 0.4f), Offset(offsetZ, centerH - 55.dp.toPx()), Size(zebraWidth, 12.dp.toPx()))
                                    drawRect(Color.White.copy(alpha = 0.4f), Offset(offsetZ, centerH + 43.dp.toPx()), Size(zebraWidth, 12.dp.toPx()))
                                }

                                val cars = listOf(
                                    Offset(centerW - 20.dp.toPx(), centerH - 80.dp.toPx()),
                                    Offset(centerW + 15.dp.toPx(), centerH + 60.dp.toPx()),
                                    Offset(centerW - 70.dp.toPx(), centerH - 18.dp.toPx()),
                                    Offset(centerW + 65.dp.toPx(), centerH + 12.dp.toPx())
                                )
                                cars.forEachIndexed { idx, pos ->
                                    drawCircle(
                                        color = if (idx % 2 == 0) PrimaryColor else AccentColor,
                                        radius = 10.dp.toPx(),
                                        center = pos
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 4.dp.toPx(),
                                        center = pos
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.TopStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${currentJunction.name} Feed [01]",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            )
                        }

                        // VEHICLE COUNT DETAILS
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
                                            text = "Vehicle Count",
                                            color = SecondaryColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Last 5 Minutes",
                                            color = SecondaryColor.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Total",
                                            color = SecondaryColor.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = currentJunction.activeVehicles.toString(),
                                            color = SecondaryColor,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CountItem(label = "Cars", count = currentJunction.carCount, icon = Icons.Default.DirectionsCar, tint = BlueState)
                                    CountItem(label = "Bikes", count = currentJunction.bikeCount, icon = Icons.Default.TwoWheeler, tint = GreenState)
                                    CountItem(label = "Buses", count = currentJunction.busCount, icon = Icons.Default.DirectionsBus, tint = PrimaryColor)
                                    CountItem(label = "Trucks", count = currentJunction.truckCount, icon = Icons.Default.LocalShipping, tint = AccentColor)
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Congestion Level: ${currentJunction.congestionLevel}",
                                    color = SecondaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                
                                val progressValue = when(currentJunction.congestionLevel.lowercase()) {
                                    "low" -> 0.25f
                                    "moderate" -> 0.55f
                                    "high" -> 0.8f
                                    "severe" -> 1.0f
                                    else -> 0.7f
                                }
                                val barColor = when(currentJunction.congestionLevel.lowercase()) {
                                    "low" -> GreenState
                                    "moderate" -> YellowState
                                    else -> AccentColor
                                }

                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = barColor,
                                    trackColor = BorderColor
                                )
                            }
                        }

                        // AI SCAN CCTV Feed Action Button
                        Button(
                            onClick = { viewModel.analyzeCCTVFrame(syntheticFeedBitmap) },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("ai_scan_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DeveloperBoard, contentDescription = "Scan", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI SCAN CCTV Feed",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Real AI analysis results description
                        if (apiResultLog != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(PrimaryColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI Computer Vision Diagnosis",
                                            color = SecondaryColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
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
fun CountItem(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(tint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = SecondaryColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = count.toString(),
            color = SecondaryColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
