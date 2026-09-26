package com.example.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.HudButton
import com.example.ui.components.HudCard
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisHoloLight
import com.example.ui.theme.StatusOnline
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.MainTab

@Composable
fun CameraVisionScreen(viewModel: JarvisViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val isFrontCamera by viewModel.cameraManager.isFrontCamera.collectAsState()
    val isAnalyzing by viewModel.isVisionAnalyzing.collectAsState()
    val visionAnalysis by viewModel.visionAnalysis.collectAsState()

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    LaunchedEffect(isFrontCamera) {
        viewModel.cameraManager.startCamera(lifecycleOwner, previewView, isFrontCamera)
    }

    BackHandler {
        viewModel.setActiveTab(MainTab.CORE)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_laser"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setActiveTab(MainTab.CORE) },
                modifier = Modifier
                    .size(38.dp)
                    .background(CyberDark, CircleShape)
                    .border(1.dp, CyberBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "OPTICAL SENSOR ARRAY",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isFrontCamera) "ACTIVE: FRONT LENS (SELFIE)" else "ACTIVE: PRIMARY REAR LENS",
                    color = StatusOnline,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            // Switch Camera Button
            IconButton(
                onClick = {
                    viewModel.cameraManager.toggleCameraFacing(lifecycleOwner, previewView)
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(JarvisCyan.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, JarvisCyan, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Camera Viewfinder with Sci-Fi Reticle Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(CyberDark, RoundedCornerShape(16.dp))
                .border(1.5.dp, JarvisCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // CameraX Viewfinder Surface
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Sci-Fi HUD Canvas Overlay (Crosshairs, brackets, scan line)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)

                // Corner Targeting Brackets
                val bracketSize = 24.dp.toPx()
                val bracketStroke = 3f

                // Top-Left
                drawLine(JarvisCyan, Offset(16f, 16f), Offset(16f + bracketSize, 16f), bracketStroke)
                drawLine(JarvisCyan, Offset(16f, 16f), Offset(16f, 16f + bracketSize), bracketStroke)

                // Top-Right
                drawLine(JarvisCyan, Offset(w - 16f, 16f), Offset(w - 16f - bracketSize, 16f), bracketStroke)
                drawLine(JarvisCyan, Offset(w - 16f, 16f), Offset(w - 16f, 16f + bracketSize), bracketStroke)

                // Bottom-Left
                drawLine(JarvisCyan, Offset(16f, h - 16f), Offset(16f + bracketSize, h - 16f), bracketStroke)
                drawLine(JarvisCyan, Offset(16f, h - 16f), Offset(16f, h - 16f - bracketSize), bracketStroke)

                // Bottom-Right
                drawLine(JarvisCyan, Offset(w - 16f, h - 16f), Offset(w - 16f - bracketSize, h - 16f), bracketStroke)
                drawLine(JarvisCyan, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - bracketSize), bracketStroke)

                // Central Reticle
                drawCircle(
                    color = JarvisCyan.copy(alpha = 0.5f),
                    center = center,
                    radius = 32.dp.toPx(),
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = JarvisCyan,
                    center = center,
                    radius = 3.dp.toPx()
                )

                // Horizontal Scan Laser
                val laserY = h * scanY
                drawLine(
                    color = JarvisCyan.copy(alpha = 0.45f),
                    start = Offset(0f, laserY),
                    end = Offset(w, laserY),
                    strokeWidth = 2f
                )
            }

            // Top-right live telemetry badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(CyberBlack.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "RESOLUTION 1080P // 60 FPS",
                    color = JarvisHoloLight,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HudButton(
                text = if (isFrontCamera) "Switch to Rear" else "Switch to Front",
                icon = Icons.Default.Cameraswitch,
                onClick = {
                    viewModel.cameraManager.toggleCameraFacing(lifecycleOwner, previewView)
                },
                modifier = Modifier.weight(1f)
            )

            HudButton(
                text = if (isAnalyzing) "Scanning..." else "Scan & Analyze",
                icon = Icons.Default.CenterFocusStrong,
                isPrimary = true,
                onClick = {
                    viewModel.cameraManager.captureFrameForVision(
                        onSuccess = { base64 ->
                            viewModel.analyzeCameraSnapshot(base64)
                        },
                        onError = { err ->
                            viewModel.addLog("ERROR", "Capture failed: ${err.message}")
                        }
                    )
                },
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vision Telemetry & AI Description Card
        HudCard(
            title = "Neural Optical Readout",
            statusBadge = if (isAnalyzing) "PROCESSING" else "ANALYZED"
        ) {
            if (isAnalyzing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = JarvisCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Analyzing visual telemetry...",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = visionAnalysis,
                        color = JarvisHoloLight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
