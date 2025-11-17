package com.example.dailyflow.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage
import com.example.dailyflow.util.Permissions
import com.example.dailyflow.util.NotificationHelper

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Permissions.hasPostNotifications(context)) return
        val name = intent.getStringExtra("name") ?: "Timer"
        val notif = NotificationHelper.createNotificationBuilder(context, "timers")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$name finished ⏰")
            .setContentText("Time's up")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notif)
        } catch (_: SecurityException) {}
    }
}


