package com.example.dailyflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DailyFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createChannel("hydration", "Hydration Reminders")
        createChannel("sleep", "Sleep Alarm")
        createChannel("timers", "Timer Alerts")
    }

    private fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
