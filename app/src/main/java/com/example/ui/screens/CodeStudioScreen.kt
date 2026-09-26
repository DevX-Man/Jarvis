package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HudButton
import com.example.ui.components.HudCard
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.CodeKeyword
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
fun CodeStudioScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val currentCode by viewModel.currentCode.collectAsState()
    val codeLanguage by viewModel.codeLanguage.collectAsState()
    val codeOutput by viewModel.codeOutput.collectAsState()

    var promptInput by remember { mutableStateOf("") }
    val languages = listOf("Kotlin", "Python", "JavaScript", "HTML", "C++", "Shell")

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
                    text = "NEURAL CODING WORKBENCH",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SYNTAX HIGHLIGHTING & STARK SANDBOX",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            // Copy Code Button
            IconButton(
                onClick = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("JarvisCode", currentCode))
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(CyberSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Language Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { lang ->
                val isSelected = lang.equals(codeLanguage, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) JarvisCyan.copy(alpha = 0.2f) else CyberDark,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) JarvisCyan else CyberBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.updateLanguage(lang) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = lang.uppercase(),
                        color = if (isSelected) JarvisCyan else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Code Editor / Display Area with Line Numbers
        HudCard(
            title = "SOURCE: ${codeLanguage.uppercase()} RUNTIME",
            statusBadge = "EDITABLE"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(CodeBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers
                    val lineCount = currentCode.lines().size.coerceAtLeast(1)
                    val lineNumbers = (1..lineCount).joinToString("\n") { it.toString().padStart(2, '0') }
                    Text(
                        text = lineNumbers,
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Code Editor Text Field
                    OutlinedTextField(
                        value = currentCode,
                        onValueChange = { viewModel.updateCode(it) },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = JarvisHoloLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = JarvisCyan
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Execution & Save Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HudButton(
                text = "Run Code",
                icon = Icons.Default.PlayArrow,
                isPrimary = true,
                color = StatusOnline,
                onClick = {
                    viewModel.runCodeSnippet(currentCode, codeLanguage)
                },
                modifier = Modifier.weight(1f)
            )

            HudButton(
                text = "Save to Vault",
                icon = Icons.Default.Save,
                isPrimary = false,
                color = JarvisCyan,
                onClick = {
                    viewModel.saveCurrentCodeToVault()
                    Toast.makeText(context, "Saved to Data Vault", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Code Prompt Generator Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark, RoundedCornerShape(10.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Tell Jarvis to generate or modify code...",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = JarvisHoloLight,
                    unfocusedTextColor = JarvisHoloLight,
                    cursorColor = JarvisCyan
                )
            )

            IconButton(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        viewModel.processCommand("Write code: $promptInput in $codeLanguage")
                        promptInput = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(JarvisCyan.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Generate",
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Virtual Sandbox Execution Output
        HudCard(
            title = "Virtual Sandbox Output",
            statusBadge = "TERMINAL"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(CodeBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = codeOutput,
                    color = CodeKeyword,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
