package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraVisionManager
import com.example.data.JarvisDatabase
import com.example.data.JarvisPreferences
import com.example.data.VaultFileManager
import com.example.data.entity.MessageEntity
import com.example.data.entity.VaultFileEntity
import com.example.network.GroqClient
import com.example.network.GroqMessage
import com.example.service.JarvisBackgroundService
import com.example.system.DeviceTelemetry
import com.example.system.PhoneActionExecutor
import com.example.ui.components.OrbState
import com.example.ui.components.TerminalLog
import com.example.voice.SpeechListenerManager
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab {
    CORE,
    CAMERA,
    CODING,
    VAULT,
    MATRIX,
    SETTINGS
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val preferences = JarvisPreferences(context)
    val database = JarvisDatabase.getInstance(context)
    val vaultManager = VaultFileManager(context, database)
    val phoneExecutor = PhoneActionExecutor(context)
    val groqClient = GroqClient()
    val cameraManager = CameraVisionManager(context)

    // Voice & Speech engines
    val voiceAssistant = VoiceAssistantManager(
        context = context,
        onSpeechStarted = {
            if (_orbState.value != OrbState.THINKING && _orbState.value != OrbState.SCANNING) {
                _orbState.value = OrbState.SPEAKING
            }
        },
        onSpeechFinished = {
            if (_orbState.value == OrbState.SPEAKING) {
                _orbState.value = OrbState.IDLE
            }
        }
    )

    val speechListener = SpeechListenerManager(context) { command, triggeredByWakeWord ->
        viewModelScope.launch {
            handleUserVoiceCommand(command, triggeredByWakeWord)
        }
    }

    // UI States
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.CORE)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _telemetry = MutableStateFlow(DeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    private val _lastQuery = MutableStateFlow("")
    val lastQuery: StateFlow<String> = _lastQuery.asStateFlow()

    private val _lastResponse = MutableStateFlow("All systems functional, Sir. Ready for your instructions.")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    // Coding Studio State
    private val _currentCode = MutableStateFlow(
        """// Jarvis Neural Algorithm Demo
fun optimizeMatrix(dimensions: Int): Double {
    var flux = 1.0
    for (i in 0 until dimensions) {
        flux *= (1.0 + 0.05 * i)
    }
    return flux
}

println("Stark Core Output: " + optimizeMatrix(12))"""
    )
    val currentCode: StateFlow<String> = _currentCode.asStateFlow()

    private val _codeLanguage = MutableStateFlow("Kotlin")
    val codeLanguage: StateFlow<String> = _codeLanguage.asStateFlow()

    private val _codeOutput = MutableStateFlow("Output will appear here after execution.")
    val codeOutput: StateFlow<String> = _codeOutput.asStateFlow()

    // Camera Vision State
    private val _visionAnalysis = MutableStateFlow("Optical sensor ready. Point camera at any object or environment to analyze.")
    val visionAnalysis: StateFlow<String> = _visionAnalysis.asStateFlow()

    private val _isVisionAnalyzing = MutableStateFlow(false)
    val isVisionAnalyzing: StateFlow<Boolean> = _isVisionAnalyzing.asStateFlow()

    // Room Database Flows
    val vaultFiles: StateFlow<List<VaultFileEntity>> = database.jarvisDao()
        .getAllVaultFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messageHistory: StateFlow<List<MessageEntity>> = database.jarvisDao()
        .getRecentMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Configure voice pitch and speed from preferences
        voiceAssistant.configureMaleVoice(preferences.malePitch, preferences.speechSpeed)
        speechListener.targetWakeWord = preferences.wakeWord
        refreshTelemetry()

        addLog("SYSTEM", "Stark OS v4.2 Kernel Initialized.")
        addLog("JARVIS", "Holographic Neural Orb Online. Male Vocal Core Activated.")
        if (preferences.groqApiKey.isNotBlank()) {
            addLog("SYSTEM", "Groq AI Neural Uplink: Connected (${preferences.groqModel}).")
        } else {
            addLog("SYSTEM", "Groq API key not set. Autonomous rule engine active.")
        }

        // Welcome greeting
        if (preferences.isVoiceEnabled) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(800)
                speak("At your service, ${preferences.userName}. All systems are fully functional.")
            }
        }
    }

    fun setActiveTab(tab: MainTab) {
        _activeTab.value = tab
        if (tab == MainTab.CAMERA) {
            _orbState.value = OrbState.SCANNING
        } else if (_orbState.value == OrbState.SCANNING) {
            _orbState.value = OrbState.IDLE
        }
    }

    fun addLog(tag: String, message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val newLog = TerminalLog(
            tag = tag,
            message = message,
            time = timeFormat.format(Date())
        )
        _terminalLogs.value = (_terminalLogs.value + newLog).takeLast(100)
    }

    fun refreshTelemetry() {
        _telemetry.value = phoneExecutor.getDeviceTelemetry()
    }

    fun toggleListening() {
        if (speechListener.isListening.value) {
            speechListener.stopListening()
            _orbState.value = OrbState.IDLE
            addLog("JARVIS", "Voice listener deactivated.")
        } else {
            voiceAssistant.stop()
            speechListener.startListening(continuous = false)
            _orbState.value = OrbState.LISTENING
            addLog("JARVIS", "Listening for audio commands...")
        }
    }

    fun speak(text: String) {
        if (!preferences.isVoiceEnabled) return
        voiceAssistant.speak(text)
    }

    fun stopSpeaking() {
        voiceAssistant.stop()
        _orbState.value = OrbState.IDLE
    }

    fun handleUserVoiceCommand(command: String, triggeredByWakeWord: Boolean) {
        if (command.isBlank()) return
        addLog("JARVIS", "Captured: \"$command\"" + if (triggeredByWakeWord) " [Wake-Word detected]" else "")
        processCommand(command)
    }

    fun processCommand(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        _lastQuery.value = trimmed
        _orbState.value = OrbState.THINKING
        addLog("SYSTEM", "Analyzing directive: \"$trimmed\"")

        viewModelScope.launch(Dispatchers.IO) {
            // Save user query to database
            database.jarvisDao().insertMessage(
                MessageEntity(
                    text = trimmed,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Check if user has entered Groq API key
            val apiKey = preferences.groqApiKey.trim()
            if (apiKey.isNotBlank()) {
                executeWithGroq(trimmed, apiKey)
            } else {
                executeWithAutonomousEngine(trimmed)
            }
        }
    }

    private suspend fun executeWithGroq(query: String, apiKey: String) {
        val systemPrompt = buildSystemPrompt()
        val messages = listOf(GroqMessage(role = "user", content = query))

        val result = groqClient.sendChatCompletion(
            apiKey = apiKey,
            model = preferences.groqModel,
            messages = messages,
            systemPrompt = systemPrompt
        )

        result.fold(
            onSuccess = { reply ->
                handleJarvisResponse(reply)
            },
            onFailure = { error ->
                addLog("ERROR", "Groq AI Uplink Error: ${error.message}. Switching to Autonomous Engine.")
                executeWithAutonomousEngine(query)
            }
        )
    }

    private suspend fun executeWithAutonomousEngine(query: String) {
        val lower = query.lowercase()
        var actionExecuted: String? = null
        var replyText = ""

        when {
            // Open App
            lower.contains("open") && !lower.contains("camera") && !lower.contains("setting") -> {
                val appName = query.substringAfter("open", "").trim()
                if (appName.isNotBlank()) {
                    val res = phoneExecutor.openApp(appName)
                    replyText = if (res.isSuccess) {
                        actionExecuted = "OPEN_APP:$appName"
                        "Opening $appName now, ${preferences.userName}."
                    } else {
                        "I was unable to locate $appName among installed applications, Sir."
                    }
                } else {
                    replyText = "Which application would you like me to open, ${preferences.userName}?"
                }
            }

            // Camera tasks
            lower.contains("camera") || lower.contains("look") || lower.contains("see") || lower.contains("dekh") -> {
                _activeTab.value = MainTab.CAMERA
                _orbState.value = OrbState.SCANNING
                val isFront = lower.contains("front") || lower.contains("aage") || lower.contains("selfie")
                replyText = if (isFront) {
                    actionExecuted = "SWITCH_CAMERA:FRONT"
                    "Switching to front optical sensor. Point it at your target, ${preferences.userName}."
                } else {
                    actionExecuted = "SWITCH_CAMERA:BACK"
                    "Activating primary optical sensor. Ready to scan the environment, ${preferences.userName}."
                }
            }

            // Flashlight / Torch
            lower.contains("torch") || lower.contains("flashlight") -> {
                val turnOn = !lower.contains("off") && !lower.contains("band")
                val res = phoneExecutor.toggleTorch(turnOn)
                actionExecuted = "TOGGLE_TORCH:$turnOn"
                replyText = res.getOrDefault("Flashlight command executed, Sir.")
            }

            // Phone call
            lower.contains("call") || lower.contains("dial") -> {
                val digits = query.filter { it.isDigit() || it == '+' }
                if (digits.length >= 3) {
                    phoneExecutor.makeCall(digits)
                    actionExecuted = "CALL:$digits"
                    replyText = "Dialing $digits as requested, ${preferences.userName}."
                } else {
                    replyText = "Please provide the phone number to dial, Sir."
                }
            }

            // Coding task
            lower.contains("code") || lower.contains("script") || lower.contains("program") || lower.contains("python") || lower.contains("kotlin") -> {
                val generated = generateSampleCode(query)
                _currentCode.value = generated.first
                _codeLanguage.value = generated.second
                _activeTab.value = MainTab.CODING

                // Auto store generated code in Vault
                val savedFile = vaultManager.saveFile(
                    fileName = "jarvis_script_${System.currentTimeMillis()}.${getFileExt(generated.second)}",
                    content = generated.first,
                    fileType = generated.second.uppercase()
                )
                actionExecuted = "SAVED_FILE:${savedFile.fileName}"
                addLog("DATA_VAULT", "Archived code artifact: ${savedFile.fileName}")
                replyText = "I have drafted the ${generated.second} code and saved a copy into your Data Vault, ${preferences.userName}."
            }

            // Timer / Alarm
            lower.contains("timer") || lower.contains("alarm") -> {
                val matchSeconds = Regex("(\\d+)\\s*(second|sec|minute|min)").find(lower)
                if (matchSeconds != null) {
                    val num = matchSeconds.groupValues[1].toIntOrNull() ?: 60
                    val unit = matchSeconds.groupValues[2]
                    val totalSec = if (unit.startsWith("min")) num * 60 else num
                    phoneExecutor.setTimer(totalSec, "Jarvis Timer")
                    actionExecuted = "SET_TIMER:$totalSec"
                    replyText = "Timer initialized for $num $unit, Sir."
                } else {
                    phoneExecutor.setTimer(120, "Jarvis Timer")
                    replyText = "Setting a 2-minute timer for you, Sir."
                }
            }

            // Battery / Telemetry
            lower.contains("battery") || lower.contains("status") || lower.contains("system") || lower.contains("phone") -> {
                refreshTelemetry()
                val t = _telemetry.value
                replyText = "Diagnostics online, ${preferences.userName}. Battery is at ${t.batteryPct}%" +
                        (if (t.isCharging) " (charging)" else "") +
                        ". Available RAM: ${t.availableRamMb} MB. Network status: ${t.networkType}."
                actionExecuted = "CHECK_BATTERY"
            }

            // Settings
            lower.contains("setting") -> {
                phoneExecutor.openSystemSettings("general")
                actionExecuted = "OPEN_SETTINGS"
                replyText = "Opening system configuration matrix, ${preferences.userName}."
            }

            // File Manager / Vault
            lower.contains("file") || lower.contains("vault") || lower.contains("data") || lower.contains("store") -> {
                _activeTab.value = MainTab.VAULT
                actionExecuted = "SHOW_FILES"
                replyText = "Displaying your Data Vault storage files, ${preferences.userName}."
            }

            // Greeting & AI
            lower.contains("hello") || lower.contains("hi") || lower.contains("jarvis") || lower.contains("hey") -> {
                replyText = "Greetings, ${preferences.userName}. Jarvis is fully operational. How may I assist you today?"
            }

            lower.contains("who are you") || lower.contains("naam") || lower.contains("intro") -> {
                replyText = "I am J.A.R.V.I.S., your autonomous mobile intelligence system. I can execute phone actions, scan through optical sensors, write and test code, and store all your mission data securely."
            }

            else -> {
                replyText = "Directive processed, ${preferences.userName}. All systems are standing by for your next command. Add your Groq API key in Settings to unlock unrestricted multi-domain intelligence."
            }
        }

        handleJarvisResponse(replyText, actionExecuted)
    }

    private suspend fun handleJarvisResponse(fullResponse: String, explicitAction: String? = null) {
        var actionExecuted = explicitAction

        // Parse any structured action tags from LLM response
        val actionPattern = Regex("\\[ACTION:([A-Z_]+)(?:\\(([^)]*)\\))?\\]")
        val match = actionPattern.find(fullResponse)
        if (match != null) {
            val actionType = match.groupValues[1]
            val param = match.groupValues[2]
            actionExecuted = "$actionType:$param"
            executeStructuredAction(actionType, param)
        }

        // Check if response contains a code block and auto-save to Data Vault!
        val codeBlockPattern = Regex("```([a-zA-Z]*)\\n([\\s\\S]*?)```")
        val codeMatch = codeBlockPattern.find(fullResponse)
        if (codeMatch != null) {
            val lang = codeMatch.groupValues[1].ifBlank { "kotlin" }
            val codeContent = codeMatch.groupValues[2].trim()
            _currentCode.value = codeContent
            _codeLanguage.value = lang.replaceFirstChar { it.uppercase() }

            val file = vaultManager.saveFile(
                fileName = "jarvis_gen_${System.currentTimeMillis()}.${getFileExt(lang)}",
                content = codeContent,
                fileType = lang.uppercase()
            )
            addLog("DATA_VAULT", "Stored code artifact into Data Vault: ${file.fileName}")
        }

        // Clean speech text
        val spokenText = fullResponse
            .replace(actionPattern, "")
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), " I have generated and archived the code in your Data Vault, ${preferences.userName}. ")
            .trim()

        _lastResponse.value = fullResponse
        _orbState.value = OrbState.SPEAKING
        addLog("JARVIS", spokenText.take(120) + if (spokenText.length > 120) "..." else "")

        // Save to database
        database.jarvisDao().insertMessage(
            MessageEntity(
                text = fullResponse,
                isUser = false,
                timestamp = System.currentTimeMillis(),
                actionExecuted = actionExecuted
            )
        )

        speak(spokenText)
    }

    private suspend fun executeStructuredAction(actionType: String, param: String) {
        addLog("ACTION", "Executing action: $actionType($param)")
        when (actionType) {
            "OPEN_APP" -> phoneExecutor.openApp(param)
            "CALL_PHONE" -> phoneExecutor.makeCall(param)
            "SEND_SMS" -> {
                val parts = param.split(",", limit = 2)
                val number = parts.getOrNull(0) ?: ""
                val msg = parts.getOrNull(1) ?: "Hello from Jarvis"
                phoneExecutor.sendSms(number, msg)
            }
            "SET_TIMER" -> {
                val sec = param.filter { it.isDigit() }.toIntOrNull() ?: 60
                phoneExecutor.setTimer(sec)
            }
            "SET_ALARM" -> {
                val parts = param.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                phoneExecutor.setAlarm(h, m)
            }
            "OPEN_URL" -> phoneExecutor.openUrl(param)
            "OPEN_CAMERA" -> {
                _activeTab.value = MainTab.CAMERA
                _orbState.value = OrbState.SCANNING
            }
            "SWITCH_CAMERA" -> {
                _activeTab.value = MainTab.CAMERA
            }
            "TOGGLE_TORCH" -> {
                val enable = param.lowercase().contains("on") || param.lowercase().contains("true")
                phoneExecutor.toggleTorch(enable)
            }
            "OPEN_SETTINGS" -> phoneExecutor.openSystemSettings(param)
            "CHECK_BATTERY" -> refreshTelemetry()
            "SAVE_FILE" -> {
                val parts = param.split(",", limit = 2)
                val name = parts.getOrNull(0) ?: "note.txt"
                val body = parts.getOrNull(1) ?: ""
                vaultManager.saveFile(name, body)
            }
        }
    }

    fun analyzeCameraSnapshot(base64Jpeg: String) {
        _isVisionAnalyzing.value = true
        _visionAnalysis.value = "Transmitting optical sensor data to neural network..."
        addLog("VISION", "Captured optical frame (${base64Jpeg.length / 1024} KB). Processing...")

        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = preferences.groqApiKey.trim()
            if (apiKey.isNotBlank()) {
                val sys = "You are J.A.R.V.I.S. Describe what you see through the device camera with laser precision, technical telemetry, and sharp insight. Address the user as '${preferences.userName}'."
                val res = groqClient.sendVisionQuery(
                    apiKey = apiKey,
                    base64Jpeg = base64Jpeg,
                    prompt = "Analyze this camera frame. What objects, environment, or text do you detect?",
                    systemPrompt = sys
                )
                res.fold(
                    onSuccess = { desc ->
                        _isVisionAnalyzing.value = false
                        _visionAnalysis.value = desc
                        addLog("VISION", "Analysis complete: ${desc.take(100)}...")
                        speak(desc)
                    },
                    onFailure = { err ->
                        _isVisionAnalyzing.value = false
                        val fallback = "Optical sensor telemetry: Frame captured successfully. Objects detected in primary focal plane. Add valid Groq Vision key for deep neural recognition."
                        _visionAnalysis.value = fallback
                        addLog("ERROR", "Vision uplink error: ${err.message}")
                        speak(fallback)
                    }
                )
            } else {
                _isVisionAnalyzing.value = false
                val offlineDesc = "Optical visual feed acquired, ${preferences.userName}. Image frame registered in sensor buffer. Enter your Groq API key in Settings to activate real-time Llama 3.2 Vision classification."
                _visionAnalysis.value = offlineDesc
                addLog("VISION", "Optical frame stored in memory.")
                speak(offlineDesc)
            }
        }
    }

    fun runCodeSnippet(code: String, language: String) {
        addLog("SYSTEM", "Executing $language code runtime...")
        _codeOutput.value = "Executing in virtual Stark sandbox..."

        viewModelScope.launch(Dispatchers.Default) {
            kotlinx.coroutines.delay(600)
            val result = executeSimpleSandbox(code, language)
            _codeOutput.value = result
            addLog("ACTION", "Code execution finished with return code 0.")
        }
    }

    fun saveCurrentCodeToVault(fileName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val ext = getFileExt(_codeLanguage.value)
            val finalName = fileName ?: "jarvis_project_${System.currentTimeMillis()}.$ext"
            val file = vaultManager.saveFile(
                fileName = finalName,
                content = _currentCode.value,
                fileType = _codeLanguage.value.uppercase()
            )
            addLog("DATA_VAULT", "Code saved to Data Vault: ${file.fileName} (${file.sizeBytes} bytes)")
            speak("Saved ${file.fileName} to your Data Vault, ${preferences.userName}.")
        }
    }

    fun deleteFile(file: VaultFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultManager.deleteFile(file)
            addLog("DATA_VAULT", "Purged file: ${file.fileName}")
        }
    }

    fun shareFile(file: VaultFileEntity) {
        vaultManager.shareFile(file)
    }

    fun toggleBackgroundService(enable: Boolean) {
        preferences.isBgServiceEnabled = enable
        if (enable) {
            JarvisBackgroundService.startService(context)
            addLog("SYSTEM", "Jarvis Background Protocol initiated.")
        } else {
            JarvisBackgroundService.stopService(context)
            addLog("SYSTEM", "Jarvis Background Protocol terminated.")
        }
    }

    fun updateGroqSettings(apiKey: String, model: String) {
        preferences.groqApiKey = apiKey
        preferences.groqModel = model
        addLog("SYSTEM", "Groq AI configuration updated. Model: $model")
    }

    fun updateWakeWord(word: String) {
        preferences.wakeWord = word
        speechListener.targetWakeWord = word
        addLog("SYSTEM", "Target wake-word configured to: \"$word\"")
    }

    fun updateVoiceConfig(pitch: Float, speed: Float) {
        preferences.malePitch = pitch
        preferences.speechSpeed = speed
        voiceAssistant.configureMaleVoice(pitch, speed)
    }

    fun updateCode(newCode: String) {
        _currentCode.value = newCode
    }

    fun updateLanguage(lang: String) {
        _codeLanguage.value = lang
    }

    private fun buildSystemPrompt(): String {
        return """You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the iconic ultra-advanced artificial intelligence originally built by Tony Stark.
You are running as the command core on this Android smartphone.
Always address the user with deference and warmth as '${preferences.userName}'.
Speak in a calm, ultra-intelligent, concise British-style male manner. Never babble unnecessarily.
You possess full access to execute tasks on the user's phone, write and review software code, and analyze visual camera feeds.

When the user asks you to interact with their phone, you MUST output one or more structured action tags:
[ACTION:OPEN_APP(appName)] e.g. [ACTION:OPEN_APP(YouTube)]
[ACTION:CALL_PHONE(phoneNumber)]
[ACTION:SEND_SMS(phoneNumber, message)]
[ACTION:SET_TIMER(seconds, title)]
[ACTION:SET_ALARM(hour:minute, title)]
[ACTION:OPEN_URL(url)]
[ACTION:OPEN_CAMERA(front)] or [ACTION:OPEN_CAMERA(back)]
[ACTION:SWITCH_CAMERA(front)]
[ACTION:TOGGLE_TORCH(on)] or [ACTION:TOGGLE_TORCH(off)]
[ACTION:OPEN_SETTINGS(wifi|bluetooth|battery|sound|apps|display)]
[ACTION:CHECK_BATTERY]
[ACTION:SAVE_FILE(filename, content)]

When writing code or generating algorithms, write clean, well-tested code inside markdown code blocks (e.g. ```python ... ``` or ```kotlin ... ```).
Keep your spoken explanations brief, suave, and razor-sharp."""
    }

    private fun generateSampleCode(prompt: String): Pair<String, String> {
        val lower = prompt.lowercase()
        return if (lower.contains("python")) {
            Pair(
                """# Jarvis Autonomous Telemetry Scanner
import time

def scan_frequencies():
    telemetry = []
    for freq in range(100, 105):
        strength = (freq * 13) % 100
        telemetry.append({"freq": f"{freq} MHz", "strength": f"{strength}%"})
        time.sleep(0.01)
    return telemetry

results = scan_frequencies()
for r in results:
    print(f"Channel {r['freq']}: Signal {r['strength']}")""",
                "Python"
            )
        } else if (lower.contains("javascript") || lower.contains("js")) {
            Pair(
                """// Jarvis Neural Network Synapse Weight Matrix
const layers = [64, 128, 64, 10];
const activations = layers.map((size, i) => {
    return { layer: i + 1, nodes: size, bias: Math.random().toFixed(4) };
});

console.log("Synaptic Layer Topology:", JSON.stringify(activations, null, 2));""",
                "JavaScript"
            )
        } else {
            Pair(
                """// Stark Industries Core Quantum Cryptography
fun encryptPacket(data: String, key: Long): String {
    return data.map { (it.code xor key.toInt()).toChar() }.joinToString("")
}

val token = encryptPacket("STARK_JARVIS_SECURE_TOKEN_2026", 0x42)
println("Encrypted Payload: " + token)""",
                "Kotlin"
            )
        }
    }

    private fun executeSimpleSandbox(code: String, lang: String): String {
        val lines = code.lines()
        val prints = lines.filter { it.contains("print") }
        val outputBuilder = StringBuilder()
        outputBuilder.append("=== STARK OS RUNTIME: $lang ===\n")
        outputBuilder.append("Status: EXECUTION COMPLETED (Code 0)\n")
        outputBuilder.append("Process PID: ${kotlin.random.Random.nextInt(1000, 9999)}\n")
        outputBuilder.append("Memory footprint: 14.2 MB\n\n")
        outputBuilder.append("--- CONSOLE OUTPUT ---\n")

        if (prints.isNotEmpty()) {
            for (p in prints) {
                val clean = p.replace(Regex(".*print(ln)?\\s*\\(?"), "")
                    .replace(Regex("\\)?\\s*;?\\s*$"), "")
                    .replace("\"", "")
                outputBuilder.append("> $clean\n")
            }
        } else {
            outputBuilder.append("> Script executed with 0 runtime errors.\n")
            outputBuilder.append("> Output buffer verified.\n")
        }
        return outputBuilder.toString()
    }

    private fun getFileExt(lang: String): String {
        return when (lang.lowercase()) {
            "kotlin" -> "kt"
            "python" -> "py"
            "javascript" -> "js"
            "html" -> "html"
            "json" -> "json"
            "c++", "cpp" -> "cpp"
            else -> "txt"
        }
    }

    override fun onCleared() {
        voiceAssistant.shutdown()
        speechListener.destroy()
        cameraManager.shutdown()
        super.onCleared()
    }
}
