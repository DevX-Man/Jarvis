package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HudButton
import com.example.ui.components.HudCard
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisHoloLight
import com.example.ui.theme.StatusOnline
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.MainTab

@Composable
fun SettingsScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val prefs = viewModel.preferences

    var groqApiKey by remember { mutableStateOf(prefs.groqApiKey) }
    var selectedModel by remember { mutableStateOf(prefs.groqModel) }
    var wakeWord by remember { mutableStateOf(prefs.wakeWord) }
    var userName by remember { mutableStateOf(prefs.userName) }

    var malePitch by remember { mutableFloatStateOf(prefs.malePitch) }
    var speechSpeed by remember { mutableFloatStateOf(prefs.speechSpeed) }

    var showApiKey by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    val groqModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "llama-3.2-11b-vision-preview",
        "mixtral-8x7b-32768"
    )

    BackHandler {
        viewModel.setActiveTab(MainTab.CORE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
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
                    text = "SYSTEM CONFIGURATION MATRIX",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "GROQ API & VOCAL PROFILE",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Groq Cloud AI Integration Card
        HudCard(
            title = "Groq AI Neural Uplink (Free Tier)",
            statusBadge = if (groqApiKey.isNotBlank()) "CONFIGURED" else "OFFLINE"
        ) {
            Text(
                text = "Enter your free-tier Groq API Key below (e.g. gsk_...). Groq provides ultra-fast Llama-3 inference with zero latency.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // API Key Input
            OutlinedTextField(
                value = groqApiKey,
                onValueChange = { groqApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Groq API Key (gsk_...)") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = JarvisCyan
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = JarvisHoloLight,
                    unfocusedTextColor = JarvisHoloLight,
                    focusedLabelColor = JarvisCyan,
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Model Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Groq LLM Model") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { modelMenuExpanded = true },
                    trailingIcon = {
                        IconButton(onClick = { modelMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Select Model",
                                tint = JarvisCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = JarvisHoloLight,
                        unfocusedTextColor = JarvisHoloLight
                    )
                )

                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false },
                    modifier = Modifier.background(CyberDark)
                ) {
                    groqModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = model,
                                    color = if (model == selectedModel) JarvisCyan else JarvisHoloLight,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                selectedModel = model
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HudButton(
                text = "Save Groq Configuration",
                icon = Icons.Default.CheckCircle,
                isPrimary = true,
                onClick = {
                    viewModel.updateGroqSettings(groqApiKey, selectedModel)
                    Toast.makeText(context, "Groq credentials verified & saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Wake-Up Word Configuration
        HudCard(
            title = "Wake-Up Protocol Trigger",
            statusBadge = "VOICE ACTIVATION"
        ) {
            Text(
                text = "Specify the custom keyword to activate Jarvis. Speak this word anytime to command your phone.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = wakeWord,
                onValueChange = { wakeWord = it },
                label = { Text("Wake-Up Word") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = JarvisHoloLight,
                    unfocusedTextColor = JarvisHoloLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("User Callsign / Title (e.g. Sir, Tony)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = JarvisHoloLight,
                    unfocusedTextColor = JarvisHoloLight
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            HudButton(
                text = "Apply Protocol Changes",
                icon = Icons.Default.CheckCircle,
                onClick = {
                    viewModel.updateWakeWord(wakeWord)
                    prefs.userName = userName
                    Toast.makeText(context, "Wake-Word updated to: $wakeWord", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Male Voice Synthesizer Tuning
        HudCard(
            title = "Male Vocal Synthesizer Acoustics",
            statusBadge = "TTS ENGINE"
        ) {
            Text(
                text = "Adjust pitch and speech tempo to match the calm, crisp, British male voice of J.A.R.V.I.S.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pitch Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vocal Pitch: ${String.format("%.2f", malePitch)}x (Deep Male)",
                    color = JarvisHoloLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
            Slider(
                value = malePitch,
                onValueChange = {
                    malePitch = it
                    viewModel.updateVoiceConfig(malePitch, speechSpeed)
                },
                valueRange = 0.65f..1.2f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan,
                    inactiveTrackColor = CyberBorder
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Speed Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speech Rate: ${String.format("%.2f", speechSpeed)}x (Cadence)",
                    color = JarvisHoloLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
            Slider(
                value = speechSpeed,
                onValueChange = {
                    speechSpeed = it
                    viewModel.updateVoiceConfig(malePitch, speechSpeed)
                },
                valueRange = 0.75f..1.35f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan,
                    inactiveTrackColor = CyberBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            HudButton(
                text = "Test Male Voice Synthesis",
                icon = Icons.Default.RecordVoiceOver,
                isPrimary = false,
                onClick = {
                    viewModel.updateVoiceConfig(malePitch, speechSpeed)
                    viewModel.speak("Good day, $userName. Jarvis audio synthesis operating at peak efficiency.")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
