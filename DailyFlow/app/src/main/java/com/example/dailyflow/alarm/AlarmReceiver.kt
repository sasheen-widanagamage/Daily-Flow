package com.example.dailyflow.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailyflow.R
import com.example.dailyflow.ui.alarm.AlarmRingActivity
import com.example.dailyflow.util.AlarmScheduler
import com.example.dailyflow.util.Feedback
import com.example.dailyflow.util.Permissions


class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        const val CHANNEL_ID = "alarms"
        const val NOTIFICATION_ID_BASE = 1000
        const val SLEEP_ALARM_REQUEST_CODE = 424242
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sleep and hydration alarm notifications"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra("type") ?: return
        val username = intent.getStringExtra("username") ?: return
        
        // Check if notifications are allowed
        if (!Permissions.hasPostNotifications(context)) {
            return
        }
        
        // Create notification channel if needed
        createNotificationChannel(context)
        
        when (alarmType) {
            AlarmScheduler.ALARM_TYPE_SLEEP -> handleSleepAlarm(context, username)
            AlarmScheduler.ALARM_TYPE_HYDRATION -> handleHydrationAlarm(context, username, intent)
        }
    }
    
    private fun handleSleepAlarm(context: Context, username: String) {
        // Create notification with stop action
        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            putExtra("type", AlarmScheduler.ALARM_TYPE_SLEEP)
            putExtra("username", username)
        }
        
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            SLEEP_ALARM_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra("type", AlarmScheduler.ALARM_TYPE_SLEEP)
            putExtra("username", username)
            putExtra("title", "Sleep Time")
            putExtra("message", "Time to go to bed! 🛏️")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            SLEEP_ALARM_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Sleep Time 🛏️")
            .setContentText("Time to go to bed!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Alarm", stopPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(SLEEP_ALARM_REQUEST_CODE, notification)
        } catch (e: SecurityException) {
        }
        
        // Start the alarm ring activity
        context.startActivity(fullScreenIntent)
    }
    
    private fun handleHydrationAlarm(context: Context, username: String, intent: Intent) {
        val requestCode = intent.getIntExtra("requestCode", 0)
        
        // Create notification with stop action
        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            putExtra("type", AlarmScheduler.ALARM_TYPE_HYDRATION)
            putExtra("username", username)
            putExtra("requestCode", requestCode)
        }
        
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra("type", AlarmScheduler.ALARM_TYPE_HYDRATION)
            putExtra("username", username)
            putExtra("title", "Hydration Time")
            putExtra("message", "Time to drink some water! 💧")
            putExtra("requestCode", requestCode)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hydration Time 💧")
            .setContentText("Time to drink some water!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Alarm", stopPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        } catch (e: SecurityException) {
        }
        
        // Start the alarm ring activity
        context.startActivity(fullScreenIntent)
    }
}
