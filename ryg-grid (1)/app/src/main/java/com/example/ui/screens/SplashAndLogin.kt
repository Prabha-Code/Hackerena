package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.RYGViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    var animateLogo by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        animateLogo = true
        delay(2200)
        onNavigate("login")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable { onNavigate("login") }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Content: Flame logo + text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                // LOGO: Adaptive Organic Flame Blob matching image exactly using Canvas
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .testTag("splash_logo_container"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // 1. Draw outer flame path
                        val flamePath = Path().apply {
                            moveTo(w * 0.48f, h * 0.12f)
                            // Left-side curve down
                            cubicTo(
                                w * 0.28f, h * 0.20f,
                                w * 0.18f, h * 0.45f,
                                w * 0.22f, h * 0.68f
                            )
                            // Bottom-left to bottom-right curve
                            cubicTo(
                                w * 0.25f, h * 0.90f,
                                w * 0.75f, h * 0.92f,
                                w * 0.78f, h * 0.68f
                            )
                            // Right curve going up
                            cubicTo(
                                w * 0.82f, h * 0.45f,
                                w * 0.68f, h * 0.25f,
                                w * 0.48f, h * 0.12f
                            )
                        }
                        drawPath(
                            path = flamePath,
                            color = Color(0xFFFD2C50) // Premium Crimson/Red
                        )

                        // 2. Draw the white "S" carving on top of the red flame
                        // To draw the S stroke beautifully, we use a Path and Stroke style.
                        val sPath = Path().apply {
                            moveTo(w * 0.73f, h * 0.44f)
                            // Left curve forming the top-right entry swooping down to the center-left
                            cubicTo(
                                w * 0.58f, h * 0.45f,
                                w * 0.44f, h * 0.50f,
                                w * 0.45f, h * 0.60f
                            )
                            // Swooping down and right to form the inner loop that exits towards bottom right/left
                            cubicTo(
                                w * 0.46f, h * 0.70f,
                                w * 0.68f, h * 0.68f,
                                w * 0.58f, h * 0.81f
                            )
                            // Finish at the bottom-left edge
                            cubicTo(
                                w * 0.51f, h * 0.88f,
                                w * 0.38f, h * 0.84f,
                                w * 0.34f, h * 0.78f
                            )
                        }
                        drawPath(
                            path = sPath,
                            color = Color.White,
                            style = Stroke(
                                width = 14.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brand Text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RYG ",
                        color = Color(0xFFFD2C50),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "GRID",
                        color = Color(0xFF161C2C),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "SMARTER SIGNALS, SAFER ROADS",
                    color = Color(0xFF8A96A0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // Lower Content: Full width city scene vector illustration
            BottomTrafficIllustration()
        }
    }
}

@Composable
fun BottomTrafficIllustration() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .testTag("traffic_illustration")
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw City Skyline (Faint gray/beige silhouettes of skyscrapers in the background)
        val skylineY = h * 0.55f
        val skylineColor = Color(0xFFEFF1F4).copy(alpha = 0.8f)
        
        // Building 1 (Far left, standard high-rise)
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.05f, h * 0.25f),
            size = Size(w * 0.08f, h * 0.30f)
        )
        // Building 2 (Wide building with antenna)
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.15f, h * 0.18f),
            size = Size(w * 0.12f, h * 0.37f)
        )
        // Antenna
        drawLine(
            color = skylineColor,
            start = Offset(w * 0.21f, h * 0.10f),
            end = Offset(w * 0.21f, h * 0.18f),
            strokeWidth = 2.dp.toPx()
        )
        
        // Building 3 (Pointy top)
        val towerPath1 = Path().apply {
            moveTo(w * 0.30f, h * 0.55f)
            lineTo(w * 0.30f, h * 0.28f)
            lineTo(w * 0.34f, h * 0.20f) // pointed tip
            lineTo(w * 0.38f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.55f)
            close()
        }
        drawPath(path = towerPath1, color = skylineColor)

        // Building 4 (Medium tall, notched)
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.42f, h * 0.32f),
            size = Size(w * 0.10f, h * 0.23f)
        )
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.44f, h * 0.28f),
            size = Size(w * 0.06f, h * 0.04f)
        )

        // Building 5 (Tall cylindrical shape)
        drawRoundRect(
            color = skylineColor,
            topLeft = Offset(w * 0.56f, h * 0.15f),
            size = Size(w * 0.11f, h * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        // Building 6 (Modern multi-tier tower)
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.70f, h * 0.22f),
            size = Size(w * 0.09f, h * 0.33f)
        )
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.72f, h * 0.15f),
            size = Size(w * 0.05f, h * 0.07f)
        )
        
        // Building 7 (Far right, simple)
        drawRect(
            color = skylineColor,
            topLeft = Offset(w * 0.82f, h * 0.30f),
            size = Size(w * 0.10f, h * 0.25f)
        )

        // 2. Draw Road and Horizon
        drawLine(
            color = Color(0xFFEFF1F4),
            start = Offset(0f, skylineY),
            end = Offset(w, skylineY),
            strokeWidth = 1.dp.toPx()
        )
        
        // Grey road filling the bottom area
        drawRect(
            color = Color(0xFFF6F8FA),
            topLeft = Offset(0f, skylineY),
            size = Size(w, h - skylineY)
        )

        // Lane dividers (subtle perspective lines or dashes)
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(0f, h * 0.76f),
            end = Offset(w, h * 0.76f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(0f, h * 0.88f),
            end = Offset(w, h * 0.88f),
            strokeWidth = 2.dp.toPx()
        )

        // 3. Draw Streetlamp on the Left
        val lampX = w * 0.10f
        val lampYStart = h * 0.28f
        val lampYEnd = h * 0.75f
        
        // Vertical post
        drawLine(
            color = Color(0xFF7A8795),
            start = Offset(lampX, lampYStart),
            end = Offset(lampX, lampYEnd),
            strokeWidth = 2.5f.dp.toPx()
        )
        // Curved top of the lamp arm
        val lampCurvePath = Path().apply {
            moveTo(lampX, lampYStart)
            cubicTo(lampX, lampYStart - 15.dp.toPx(), lampX + 15.dp.toPx(), lampYStart - 18.dp.toPx(), lampX + 22.dp.toPx(), lampYStart - 12.dp.toPx())
        }
        drawPath(
            path = lampCurvePath,
            color = Color(0xFF7A8795),
            style = Stroke(width = 2.5f.dp.toPx())
        )
        // Lamp head light source
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = 3.dp.toPx(),
            center = Offset(lampX + 22.dp.toPx(), lampYStart - 10.dp.toPx())
        )

        // Smaller lamp in perspective
        val lampX2 = w * 0.32f
        val lampYStart2 = h * 0.38f
        val lampYEnd2 = h * 0.72f
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(lampX2, lampYStart2),
            end = Offset(lampX2, lampYEnd2),
            strokeWidth = 1.5f.dp.toPx()
        )

        // 4. Vehicles (Drawn at precise horizontal lanes)
        // SUV Left (Foreground)
        drawStylizedVehicle(this, x = w * 0.04f, y = h * 0.72f, width = 64.dp.toPx(), height = 36.dp.toPx(), isBus = false, color = Color(0xFFFCFCFD))
        
        // Sedan middle-left (Mid-ground)
        drawStylizedVehicle(this, x = w * 0.36f, y = h * 0.65f, width = 45.dp.toPx(), height = 26.dp.toPx(), isBus = false, color = Color(0xFFFCFCFD))
        
        // Distant small sedan (Far-ground)
        drawStylizedVehicle(this, x = w * 0.50f, y = h * 0.63f, width = 34.dp.toPx(), height = 20.dp.toPx(), isBus = false, color = Color(0xFFEBECEF))

        // Bus on the Right (Foreground)
        drawStylizedVehicle(this, x = w * 0.58f, y = h * 0.70f, width = 72.dp.toPx(), height = 52.dp.toPx(), isBus = true, color = Color(0xFFFCFCFD))

        // 5. Draw the iconic Traffic Light Pole on the Right
        val tlX = w * 0.85f
        val tlYTop = h * 0.15f
        val tlYBottom = h * 0.82f
        
        // Vertical black pole
        drawLine(
            color = Color(0xFF2C3238),
            start = Offset(tlX, tlYTop),
            end = Offset(tlX, tlYBottom),
            strokeWidth = 5.dp.toPx()
        )
        // Ground base of the pole
        val baseWidth = 16.dp.toPx()
        drawRect(
            color = Color(0xFF1E2226),
            topLeft = Offset(tlX - baseWidth / 2, tlYBottom - 10.dp.toPx()),
            size = Size(baseWidth, 10.dp.toPx())
        )

        // Traffic Light Housing (rounded vertical dark rectangle)
        val hsW = 28.dp.toPx()
        val hsH = 76.dp.toPx()
        val hsX = tlX - hsW / 2
        val hsY = tlYTop + 10.dp.toPx()
        
        drawRoundRect(
            color = Color(0xFF1C1E22),
            topLeft = Offset(hsX, hsY),
            size = Size(hsW, hsH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Little dark borders / visors
        val visorW = 32.dp.toPx()
        val visorH = 6.dp.toPx()
        for (i in 0..2) {
            val visorY = hsY + 8.dp.toPx() + i * 22.dp.toPx() - 3.dp.toPx()
            drawRoundRect(
                color = Color(0xFF2D3136),
                topLeft = Offset(tlX - visorW/2, visorY),
                size = Size(visorW, visorH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Luminous active lights: Red (top), Yellow (middle), Green (bottom)
        val lightR = 7.dp.toPx()
        val redCenter = Offset(tlX, hsY + 12.dp.toPx() + lightR)
        val yellowCenter = Offset(tlX, hsY + 34.dp.toPx() + lightR)
        val greenCenter = Offset(tlX, hsY + 56.dp.toPx() + lightR)

        // Glow effects (glowing feel in screenshot)
        drawCircle(color = Color(0x33FF1F48), radius = lightR * 2f, center = redCenter)
        drawCircle(color = Color(0x33FFB200), radius = lightR * 2f, center = yellowCenter)
        drawCircle(color = Color(0x3300E676), radius = lightR * 2f, center = greenCenter)

        // Core bright lamps
        drawCircle(color = Color(0xFFFF1F48), radius = lightR, center = redCenter)
        drawCircle(color = Color(0xFFFFB200), radius = lightR, center = yellowCenter)
        drawCircle(color = Color(0xFF00E676), radius = lightR, center = greenCenter)
    }
}

fun drawStylizedVehicle(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    isBus: Boolean,
    color: Color
) {
    with(drawScope) {
        if (!isBus) {
            // Draw CAR
            // 1. Shadow underneath
            drawOval(
                color = Color(0x22000000),
                topLeft = Offset(x, y + height * 0.8f),
                size = Size(width, height * 0.2f)
            )

            // 2. Wheels
            val wheelW = width * 0.15f
            val wheelH = height * 0.25f
            drawRoundRect(
                color = Color(0xFF1E2226),
                topLeft = Offset(x + width * 0.08f, y + height * 0.7f),
                size = Size(wheelW, wheelH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF1E2226),
                topLeft = Offset(x + width * 0.77f, y + height * 0.7f),
                size = Size(wheelW, wheelH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // 3. Main Lower Base Body
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y + height * 0.45f),
                size = Size(width, height * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            
            // Thin dark border for contrast
            drawRoundRect(
                color = Color(0xFFBEC2C8),
                topLeft = Offset(x, y + height * 0.45f),
                size = Size(width, height * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Upper Cabin / Windshield
            val cabinW = width * 0.82f
            val cabinH = height * 0.35f
            val cabinX = x + (width - cabinW) / 2
            val cabinY = y + height * 0.15f
            
            drawRoundRect(
                color = color,
                topLeft = Offset(cabinX, cabinY),
                size = Size(cabinW, cabinH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFFBEC2C8),
                topLeft = Offset(cabinX, cabinY),
                size = Size(cabinW, cabinH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Dark Windshield glass
            drawRoundRect(
                color = Color(0xFF333E4F),
                topLeft = Offset(cabinX + cabinW * 0.08f, cabinY + cabinH * 0.12f),
                size = Size(cabinW * 0.84f, cabinH * 0.76f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 5. Headlights
            val lightRadius = width * 0.08f
            val leftLightCenter = Offset(x + width * 0.15f, y + height * 0.60f)
            val rightLightCenter = Offset(x + width * 0.85f, y + height * 0.60f)
            
            drawCircle(color = Color(0x44FFF59D), radius = lightRadius * 1.8f, center = leftLightCenter)
            drawCircle(color = Color(0x44FFF59D), radius = lightRadius * 1.8f, center = rightLightCenter)
            drawCircle(color = Color(0xFFFFF176), radius = lightRadius, center = leftLightCenter)
            drawCircle(color = Color(0xFFFFF176), radius = lightRadius, center = rightLightCenter)

            // 6. Grill & License plate
            drawRoundRect(
                color = Color(0xFF7E8994),
                topLeft = Offset(x + width * 0.35f, y + height * 0.65f),
                size = Size(width * 0.3f, height * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(x + width * 0.42f, y + height * 0.72f),
                size = Size(width * 0.16f, height * 0.08f)
            )

        } else {
            // Draw BUS
            // 1. Shadow underneath
            drawOval(
                color = Color(0x22000000),
                topLeft = Offset(x, y + height * 0.85f),
                size = Size(width, height * 0.15f)
            )

            // 2. Tires
            val tireW = width * 0.14f
            val tireH = height * 0.20f
            drawRoundRect(
                color = Color(0xFF1E2226),
                topLeft = Offset(x + width * 0.08f, y + height * 0.78f),
                size = Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF1E2226),
                topLeft = Offset(x + width * 0.78f, y + height * 0.78f),
                size = Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 3. Main Tall rectangular body
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y + height * 0.1f),
                size = Size(width, height * 0.75f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFFBEC2C8),
                topLeft = Offset(x, y + height * 0.1f),
                size = Size(width, height * 0.75f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Giant Windshield
            val wsW = width * 0.86f
            val wsH = height * 0.32f
            drawRoundRect(
                color = Color(0xFF333E4F),
                topLeft = Offset(x + width * 0.07f, y + height * 0.22f),
                size = Size(wsW, wsH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Destination board
            drawRoundRect(
                color = Color(0xFF15181C),
                topLeft = Offset(x + width * 0.25f, y + height * 0.14f),
                size = Size(width * 0.5f, height * 0.07f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // 5. Headlights
            val hlRadius = width * 0.07f
            val lLight = Offset(x + width * 0.12f, y + height * 0.65f)
            val rLight = Offset(x + width * 0.88f, y + height * 0.65f)

            drawCircle(color = Color(0x44FFF59D), radius = hlRadius * 1.8f, center = lLight)
            drawCircle(color = Color(0x44FFF59D), radius = hlRadius * 1.8f, center = rLight)
            drawCircle(color = Color(0xFFFFF176), radius = hlRadius, center = lLight)
            drawCircle(color = Color(0xFFFFF176), radius = hlRadius, center = rLight)

            // Indicator lights
            drawCircle(
                color = Color(0xFFFF9800),
                radius = 2.dp.toPx(),
                center = Offset(x + width * 0.08f, y + height * 0.72f)
            )
            drawCircle(
                color = Color(0xFFFF9800),
                radius = 2.dp.toPx(),
                center = Offset(x + width * 0.92f, y + height * 0.72f)
            )

            // Grille
            drawRect(
                color = Color(0xFF5A6572),
                topLeft = Offset(x + width * 0.32f, y + height * 0.64f),
                size = Size(width * 0.36f, height * 0.08f)
            )
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: RYGViewModel,
    onNavigate: (String) -> Unit
) {
    var email by remember { mutableStateOf("akfire87@gmail.com") }
    var password by remember { mutableStateOf("••••••••") }
    var selectedRole by remember { mutableStateOf("Traffic Officer") } // Admin, Traffic Officer, Citizen
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Mini Logo
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.05f)
                    cubicTo(
                        size.width * 0.2f, size.height * 0.35f,
                        size.width * 0.15f, size.height * 0.65f,
                        size.width * 0.5f, size.height * 0.95f
                    )
                    cubicTo(
                        size.width * 0.85f, size.height * 0.65f,
                        size.width * 0.8f, size.height * 0.35f,
                        size.width * 0.5f, size.height * 0.05f
                    )
                }
                drawPath(path = path, color = AccentColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RYG GRID",
            color = SecondaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Text(
            text = "Smarter Signals, Safer Roads",
            color = SecondaryColor.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(36.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Operator Console Sign-In",
                    color = SecondaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email / Username") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    modifier = Modifier.fillMaxWidth().testTag("username_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = PrimaryColor,
                        focusedTextColor = SecondaryColor,
                        unfocusedTextColor = SecondaryColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Console Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = PrimaryColor,
                        focusedTextColor = SecondaryColor,
                        unfocusedTextColor = SecondaryColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Role Selector Tab/Capsules
                Text(
                    text = "Select Operator Role",
                    color = SecondaryColor.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    val roles = listOf("Traffic Officer", "Admin", "Citizen")
                    roles.forEach { role ->
                        val isSelected = selectedRole == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SecondaryColor else Color.Transparent)
                                .clickable { selectedRole = role }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = role.replace("Traffic ", ""),
                                color = if (isSelected) Color.White else SecondaryColor.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        viewModel.setUsername(email)
                        viewModel.setUserRole(selectedRole)
                        viewModel.setLoggedIn(true)
                        onNavigate("dashboard")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text(
                        text = "Access Console",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
