package com.tecsup.aurora.service

import android.content.Context
import android.content.Intent
import android.os.Build

class TrackingServiceManager(private val context: Context) {

    fun startTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START_SERVICE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }
}