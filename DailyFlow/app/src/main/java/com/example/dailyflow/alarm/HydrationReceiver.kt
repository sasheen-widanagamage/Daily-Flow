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

class HydrationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (!Permissions.hasPostNotifications(context)) return

        val builder = NotificationHelper.createNotificationBuilder(context, "hydration")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hydration time 💧")
            .setContentText("Take a sip of water")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val nm = NotificationManagerCompat.from(context)
        try {
            nm.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        } catch (_: SecurityException) {
            return
        }

        NotificationHelper.vibrateOnce(context)
    }
}
