package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.JarvisPreferences
import com.example.voice.SpeechListenerManager

class JarvisBackgroundService : Service() {

    private var speechListener: SpeechListenerManager? = null
    private lateinit var prefs: JarvisPreferences

    companion object {
        const val CHANNEL_ID = "jarvis_service_channel"
        const val NOTIFICATION_ID = 4201

        fun startService(context: Context) {
            val intent = Intent(context, JarvisBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, JarvisBackgroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = JarvisPreferences(this)
        createNotificationChannel()

        speechListener = SpeechListenerManager(this) { command, triggeredByWakeWord ->
            if (triggeredByWakeWord) {
                // Launch MainActivity when wake word is detected in background
                val launchIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("AUTO_EXECUTE_COMMAND", command)
                }
                startActivity(launchIntent)
            }
        }
        speechListener?.targetWakeWord = prefs.wakeWord
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        speechListener?.targetWakeWord = prefs.wakeWord
        speechListener?.startListening(continuous = true)

        return START_STICKY
    }

    override fun onDestroy() {
        speechListener?.destroy()
        speechListener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Background Protocol",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Jarvis AI background service and wake-word listener"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS Protocol Online")
            .setContentText("Awaiting wake-word: \"${prefs.wakeWord}\"")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
