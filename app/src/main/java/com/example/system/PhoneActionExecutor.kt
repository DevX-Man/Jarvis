package com.example.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneActionExecutor(private val context: Context) {

    private var isTorchOn = false

    fun getDeviceTelemetry(): DeviceTelemetry {
        // Battery
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val isCharging = bm?.isCharging ?: false

        // RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val availRam = memInfo.availMem / (1024 * 1024)
        val totalRam = memInfo.totalMem / (1024 * 1024)

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val availStorageGb = bytesAvailable.toDouble() / (1024 * 1024 * 1024)

        // Connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WI-FI"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "5G/LTE"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ETHERNET"
            else -> if (isConnected) "ONLINE" else "OFFLINE"
        }

        return DeviceTelemetry(
            batteryPct = batteryPct,
            isCharging = isCharging,
            availableRamMb = availRam,
            totalRamMb = totalRam,
            availableStorageGb = String.format("%.1f", availStorageGb).toDoubleOrNull() ?: 12.0,
            isNetworkConnected = isConnected,
            networkType = netType
        )
    }

    suspend fun openApp(appName: String): Result<String> = withContext(Dispatchers.Default) {
        val query = appName.trim().lowercase()
        val pm = context.packageManager

        // Known direct package shortcuts
        val commonPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "camera" to "com.google.android.GoogleCamera",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "settings" to "com.android.settings",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock",
            "calendar" to "com.google.android.calendar",
            "gmail" to "com.google.android.gm",
            "photos" to "com.google.android.apps.photos",
            "files" to "com.google.android.apps.nbu.files"
        )

        val targetPkg = commonPackages[query]
        if (targetPkg != null) {
            val intent = pm.getLaunchIntentForPackage(targetPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return@withContext Result.success("Launching $appName ($targetPkg)")
            }
        }

        // Generic search through installed applications
        val installedApps = pm.getInstalledApplications(0)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@withContext Result.success("Launching ${pm.getApplicationLabel(app)}")
                }
            }
        }

        // Fallback for settings or web search
        if (query.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return@withContext Result.success("Opened Device Settings")
        }

        Result.failure(Exception("Application '$appName' not found on device."))
    }

    fun makeCall(phoneNumber: String): Result<String> {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Dialing $phoneNumber...")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendSms(phoneNumber: String, message: String): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phoneNumber.trim()}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened messaging for $phoneNumber")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setTimer(seconds: Int, message: String = "Jarvis Timer"): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Timer set for $seconds seconds.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "Jarvis Alarm"): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Alarm configured for %02d:%02d".format(hour, minute))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openUrl(url: String): Result<String> {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Navigating to $formatted")
        } catch (e: Exception) {
            // Fallback search
            val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(url)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            Result.success("Searching Google for $url")
        }
    }

    fun toggleTorch(enable: Boolean? = null): Result<String> {
        return try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = camManager?.cameraIdList?.firstOrNull { id ->
                val chars = camManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            }

            if (cameraId == null || camManager == null) {
                return Result.failure(Exception("Flashlight hardware not detected."))
            }

            val targetState = enable ?: !isTorchOn
            camManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            Result.success("Flashlight turned ${if (isTorchOn) "ON" else "OFF"}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openSystemSettings(type: String = "general"): Result<String> {
        return try {
            val intent = when (type.lowercase()) {
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "battery" -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "sound", "audio" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened $type settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
