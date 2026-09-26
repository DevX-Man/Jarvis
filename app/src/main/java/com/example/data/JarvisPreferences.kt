package com.example.data

import android.content.Context
import android.content.SharedPreferences

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_GROQ_MODEL = "groq_model"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_MALE_PITCH = "male_pitch"
        private const val KEY_SPEECH_SPEED = "speech_speed"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_BG_SERVICE = "bg_service_enabled"
        private const val KEY_CAMERA_FACING = "camera_facing"
        private const val KEY_SOUND_FX = "sound_fx_enabled"
        private const val KEY_USER_NAME = "user_name"
    }

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ_API_KEY, value.trim()).apply()

    var groqModel: String
        get() = prefs.getString(KEY_GROQ_MODEL, "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
        set(value) = prefs.edit().putString(KEY_GROQ_MODEL, value).apply()

    var wakeWord: String
        get() = prefs.getString(KEY_WAKE_WORD, "Jarvis") ?: "Jarvis"
        set(value) = prefs.edit().putString(KEY_WAKE_WORD, value.trim()).apply()

    var malePitch: Float
        get() = prefs.getFloat(KEY_MALE_PITCH, 0.85f)
        set(value) = prefs.edit().putFloat(KEY_MALE_PITCH, value).apply()

    var speechSpeed: Float
        get() = prefs.getFloat(KEY_SPEECH_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_SPEED, value).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var isBgServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_BG_SERVICE, false)
        set(value) = prefs.edit().putBoolean(KEY_BG_SERVICE, value).apply()

    var cameraFacing: String
        get() = prefs.getString(KEY_CAMERA_FACING, "BACK") ?: "BACK"
        set(value) = prefs.edit().putString(KEY_CAMERA_FACING, value).apply()

    var isSoundFxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_FX, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_FX, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Sir") ?: "Sir"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()
}
