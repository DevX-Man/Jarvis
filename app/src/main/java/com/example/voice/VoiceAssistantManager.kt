package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechFinished: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentPitch = MutableStateFlow(0.85f)
    val currentPitch: StateFlow<Float> = _currentPitch.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            configureMaleVoice()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onSpeechStarted()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeechFinished()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeechFinished()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onSpeechFinished()
                }
            })
        }
    }

    fun configureMaleVoice(pitch: Float = 0.85f, speed: Float = 1.0f) {
        _currentPitch.value = pitch
        _speechRate.value = speed

        tts?.let { engine ->
            engine.setPitch(pitch)
            engine.setSpeechRate(speed)

            // Try selecting a male voice if available in voice list
            try {
                val availableVoices = engine.voices
                if (availableVoices != null) {
                    val maleVoice = availableVoices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        (name.contains("male") || name.contains("en-gb") || name.contains("en-in") || name.contains("en-us")) &&
                                !name.contains("female")
                    }
                    if (maleVoice != null) {
                        engine.voice = maleVoice
                    } else {
                        engine.language = Locale.ENGLISH
                    }
                } else {
                    engine.language = Locale.ENGLISH
                }
            } catch (e: Exception) {
                engine.language = Locale.ENGLISH
            }
        }
    }

    fun speak(text: String, utteranceId: String = "JARVIS_${System.currentTimeMillis()}") {
        if (!isInitialized) return
        stop()

        // Clean out markdown symbols or code blocks for clean voice delivery
        val cleanText = cleanForSpeech(text)
        if (cleanText.isBlank()) return

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun cleanForSpeech(input: String): String {
        return input
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "") // Remove action tags from speech
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), " I have generated the code snippet for you, Sir. ")
            .replace(Regex("[*#_`~>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
