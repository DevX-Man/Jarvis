package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.service.JarvisBackgroundService

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                JarvisBackgroundService.CHANNEL_ID,
                "Jarvis Background Protocol",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Jarvis AI background service and wake-word listener"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
