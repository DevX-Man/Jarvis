package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.HudCard
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisHoloLight
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.MainTab

@Composable
fun MatrixControlScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current

    var voiceEnabled by remember { mutableStateOf(viewModel.preferences.isVoiceEnabled) }
    var bgServiceEnabled by remember { mutableStateOf(viewModel.preferences.isBgServiceEnabled) }
    var soundFxEnabled by remember { mutableStateOf(viewModel.preferences.isSoundFxEnabled) }
    var autoVaultSave by remember { mutableStateOf(true) }
    var appLaunchEnabled by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(true) }

    // Runtime Permission States
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = results[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

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
                    text = "PERMISSION & CONTROL MATRIX",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "GRANULAR DIRECTIVE TOGGLES",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Permissions Matrix Card
        HudCard(
            title = "Hardware Sensor Authorizations",
            statusBadge = "SYSTEM LEVEL"
        ) {
            PermissionToggleRow(
                icon = Icons.Default.Mic,
                title = "Microphone Hardware",
                description = "Required for vocal recognition & wake-word capture.",
                isGranted = hasMicPermission,
                onRequest = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionToggleRow(
                icon = Icons.Default.CameraAlt,
                title = "Optical Sensors (Dual Camera)",
                description = "Required for front and back camera vision analysis.",
                isGranted = hasCameraPermission,
                onRequest = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Functional Directive Toggles Card
        HudCard(
            title = "Autonomous Capability Toggles",
            statusBadge = "RUNTIME"
        ) {
            ToggleControlItem(
                icon = Icons.Default.Notifications,
                title = "Background Wake-Word Listener",
                subtitle = "Keep listening for '${viewModel.preferences.wakeWord}' in background.",
                checked = bgServiceEnabled,
                onCheckedChange = { checked ->
                    bgServiceEnabled = checked
                    viewModel.toggleBackgroundService(checked)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleControlItem(
                icon = Icons.Default.RecordVoiceOver,
                title = "Male Vocal Synthesizer (TTS)",
                subtitle = "Jarvis speaks responses aloud with calm British male voice.",
                checked = voiceEnabled,
                onCheckedChange = { checked ->
                    voiceEnabled = checked
                    viewModel.preferences.isVoiceEnabled = checked
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleControlItem(
                icon = Icons.Default.PhoneAndroid,
                title = "Phone Task Automation",
                subtitle = "Allow launching apps, setting timers, and dialing contacts.",
                checked = appLaunchEnabled,
                onCheckedChange = { appLaunchEnabled = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleControlItem(
                icon = Icons.Default.Folder,
                title = "Data Vault Auto-Archiving",
                subtitle = "Automatically save generated code scripts & outputs into File Vault.",
                checked = autoVaultSave,
                onCheckedChange = { autoVaultSave = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleControlItem(
                icon = Icons.Default.FlashlightOn,
                title = "Hardware Flashlight Control",
                subtitle = "Permit Jarvis to toggle rear LED torch on command.",
                checked = torchEnabled,
                onCheckedChange = { torchEnabled = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleControlItem(
                icon = Icons.Default.Vibration,
                title = "Haptic Vibration Feedback",
                subtitle = "Vibrate when wake-word is captured or action completes.",
                checked = soundFxEnabled,
                onCheckedChange = { checked ->
                    soundFxEnabled = checked
                    viewModel.preferences.isSoundFxEnabled = checked
                }
            )
        }
    }
}

@Composable
fun PermissionToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface, RoundedCornerShape(8.dp))
            .border(1.dp, if (isGranted) StatusOnline.copy(alpha = 0.4f) else StatusWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) StatusOnline else StatusWarning,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = JarvisHoloLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGranted) "AUTHORIZED" else "DISABLED",
                        color = if (isGranted) StatusOnline else StatusWarning,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(StatusWarning.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, StatusWarning, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = onRequest,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "GRANT",
                            color = StatusWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleControlItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) JarvisCyan else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = JarvisHoloLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisCyan,
                checkedTrackColor = JarvisCyan.copy(alpha = 0.35f),
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = CyberDark
            )
        )
    }
}
