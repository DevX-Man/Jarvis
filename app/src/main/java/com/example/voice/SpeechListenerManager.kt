package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechListenerManager(
    private val context: Context,
    private val onCommandReceived: (command: String, triggeredByWakeWord: Boolean) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningContinuous = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    var targetWakeWord: String = "Jarvis"

    fun initialize() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    fun startListening(continuous: Boolean = false) {
        initialize()
        isListeningContinuous = continuous

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopListening() {
        isListeningContinuous = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        _isListening.value = false
        _audioRms.value = 0f
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Map rmsdB (-2 to 10 typical) into 0f..1f range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _audioRms.value = 0f
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioRms.value = 0f
        if (isListeningContinuous) {
            // Delay and restart if continuous wake mode is enabled
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListeningContinuous) {
                    startListening(true)
                }
            }, 800)
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _audioRms.value = 0f

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spoken = matches?.firstOrNull() ?: return
        _lastRecognizedText.value = spoken

        val cleanSpoken = spoken.trim()
        val wakePattern = targetWakeWord.trim().lowercase()

        val hasWakeWord = cleanSpoken.lowercase().contains(wakePattern) ||
                cleanSpoken.lowercase().startsWith("hey $wakePattern") ||
                cleanSpoken.lowercase().startsWith("ok $wakePattern")

        if (hasWakeWord) {
            triggerVibration()
            // Extract the query part after wake word
            val idx = cleanSpoken.lowercase().indexOf(wakePattern)
            var query = cleanSpoken.substring(idx + wakePattern.length).trim()
            if (query.startsWith(",") || query.startsWith(".")) {
                query = query.substring(1).trim()
            }
            onCommandReceived(if (query.isNotBlank()) query else "Online, Sir.", true)
        } else {
            // Direct command when user tapped mic
            onCommandReceived(cleanSpoken, false)
        }

        if (isListeningContinuous) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListeningContinuous) {
                    startListening(true)
                }
            }, 500)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: return
        _lastRecognizedText.value = partial
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
