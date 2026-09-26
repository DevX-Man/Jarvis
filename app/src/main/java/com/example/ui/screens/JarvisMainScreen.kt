package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HudCard
import com.example.ui.components.JarvisOrb
import com.example.ui.components.LiveTerminalLog
import com.example.ui.components.TelemetryChip
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisHoloLight
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.MainTab

@Composable
fun JarvisMainScreen(viewModel: JarvisViewModel) {
    val orbState by viewModel.orbState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val isListening by viewModel.speechListener.isListening.collectAsState()
    val audioRms by viewModel.speechListener.audioRms.collectAsState()
    val isSpeaking by viewModel.voiceAssistant.isSpeaking.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Telemetry Matrix Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryChip(
                icon = Icons.Default.BatteryChargingFull,
                label = "Battery",
                value = "${telemetry.batteryPct}%" + if (telemetry.isCharging) " ⚡" else "",
                accentColor = if (telemetry.batteryPct > 20) StatusOnline else StatusWarning
            )
            TelemetryChip(
                icon = Icons.Default.Memory,
                label = "RAM Avail",
                value = "${telemetry.availableRamMb} MB"
            )
            TelemetryChip(
                icon = Icons.Default.Wifi,
                label = "Network",
                value = telemetry.networkType
            )
            TelemetryChip(
                icon = Icons.Default.Security,
                label = "Groq AI",
                value = if (viewModel.preferences.groqApiKey.isNotBlank()) "ONLINE" else "LOCAL",
                accentColor = if (viewModel.preferences.groqApiKey.isNotBlank()) StatusOnline else JarvisCyan
            )
            TelemetryChip(
                icon = Icons.Default.RecordVoiceOver,
                label = "Wake Word",
                value = viewModel.preferences.wakeWord
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Glowing Holographic Blue Orb
        JarvisOrb(
            orbState = orbState,
            audioRms = if (isListening) audioRms else if (isSpeaking) 0.5f else 0f,
            size = 210.dp,
            onClick = {
                if (isSpeaking) {
                    viewModel.stopSpeaking()
                } else {
                    viewModel.toggleListening()
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Action Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionPill("▶ Open YouTube", Icons.Default.PlayArrow) {
                viewModel.processCommand("Open YouTube")
            }
            QuickActionPill("📷 Scan Camera", Icons.Default.CameraAlt) {
                viewModel.setActiveTab(MainTab.CAMERA)
            }
            QuickActionPill("⚡ Torch Toggle", Icons.Default.FlashlightOn) {
                viewModel.processCommand("Toggle torch")
            }
            QuickActionPill("💻 Write Code", Icons.Default.Code) {
                viewModel.setActiveTab(MainTab.CODING)
            }
            QuickActionPill("📁 Data Vault", Icons.Default.Folder) {
                viewModel.setActiveTab(MainTab.VAULT)
            }
            QuickActionPill("📊 System Status", Icons.Default.Memory) {
                viewModel.processCommand("Check system telemetry")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Live Speech & Intelligence Response Card
        HudCard(
            title = "Neural Synthesizer Readout",
            statusBadge = if (isSpeaking) "VOCALIZING" else "READY"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = lastResponse,
                    color = JarvisHoloLight,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Live Hologram Screen Terminal Panel
        HudCard(
            title = "Stark OS // Live Console Telemetry",
            statusBadge = "LIVE STREAM"
        ) {
            LiveTerminalLog(
                logs = terminalLogs,
                heightDp = 160
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Input Controls (Text + Mic Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark, RoundedCornerShape(14.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse Mic Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (isListening) StatusOnline.copy(alpha = 0.25f) else CyberSurface,
                        CircleShape
                    )
                    .border(
                        1.5.dp,
                        if (isListening) StatusOnline else JarvisCyan,
                        CircleShape
                    )
                    .clickable { viewModel.toggleListening() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Voice Directive",
                    tint = if (isListening) StatusOnline else JarvisCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Command Jarvis or say '${viewModel.preferences.wakeWord}'...",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.processCommand(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = JarvisHoloLight,
                    unfocusedTextColor = JarvisHoloLight,
                    cursorColor = JarvisCyan
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.processCommand(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(JarvisCyan.copy(alpha = 0.18f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Transmit",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun QuickActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(CyberSurface, RoundedCornerShape(20.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = JarvisHoloLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
