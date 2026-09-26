package com.example.system

data class DeviceTelemetry(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val availableStorageGb: Double = 0.0,
    val isNetworkConnected: Boolean = true,
    val networkType: String = "WIFI"
)
