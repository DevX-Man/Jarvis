package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraVisionScreen
import com.example.ui.screens.CodeStudioScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.JarvisMainScreen
import com.example.ui.screens.MatrixControlScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisHoloLight
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JarvisViewModel
import com.example.viewmodel.MainTab

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: JarvisViewModel = viewModel()
                val context = LocalContext.current

                // Check permissions early
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions updated */ }

                LaunchedEffect(Unit) {
                    val permissionsToAsk = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToAsk.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToAsk.add(Manifest.permission.CAMERA)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToAsk.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (permissionsToAsk.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToAsk.toTypedArray())
                    }

                    // Check for background intent command
                    intent?.getStringExtra("AUTO_EXECUTE_COMMAND")?.let { cmd ->
                        viewModel.handleUserVoiceCommand(cmd, true)
                    }
                }

                JarvisAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun JarvisAppContent(viewModel: JarvisViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            JarvisBottomNavigation(
                currentTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBlack)
        ) {
            when (activeTab) {
                MainTab.CORE -> JarvisMainScreen(viewModel)
                MainTab.CAMERA -> CameraVisionScreen(viewModel)
                MainTab.CODING -> CodeStudioScreen(viewModel)
                MainTab.VAULT -> FileManagerScreen(viewModel)
                MainTab.MATRIX -> MatrixControlScreen(viewModel)
                MainTab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun JarvisBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = CyberDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = CyberBorder)
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.CORE,
            onClick = { onTabSelected(MainTab.CORE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = "Core Orb",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "ORB",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.CAMERA,
            onClick = { onTabSelected(MainTab.CAMERA) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Vision",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "VISION",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.CODING,
            onClick = { onTabSelected(MainTab.CODING) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Coding",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "CODE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.VAULT,
            onClick = { onTabSelected(MainTab.VAULT) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Data Vault",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "VAULT",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.MATRIX,
            onClick = { onTabSelected(MainTab.MATRIX) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Matrix Controls",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "MATRIX",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == MainTab.SETTINGS,
            onClick = { onTabSelected(MainTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = {
                Text(
                    "CONFIG",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}
