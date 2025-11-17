package com.example.dailyflow.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.dailyflow.util.AlarmScheduler
import com.example.dailyflow.util.Feedback

class AlarmStopReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra("type") ?: return
        val username = intent.getStringExtra("username") ?: return
        
        // Stop all feedback
        Feedback.stopAlarmFeedback()
        
        // Dismiss notifications
        val notificationManager = NotificationManagerCompat.from(context)
        
        when (alarmType) {
            AlarmScheduler.ALARM_TYPE_SLEEP -> {
                // Cancel sleep alarm notification
                notificationManager.cancel(424242)
                
                // Cancel the sleep alarm (disable it)
                AlarmScheduler.cancelSleepAlarm(context)
                
                // Update sleep config to disabled
                val sleepConfig = com.example.dailyflow.data.PrefsManager.getSleepConfig(context, username)
                if (sleepConfig != null) {
                    com.example.dailyflow.data.PrefsManager.setSleepConfig(
                        context, 
                        username, 
                        sleepConfig.copy(enabled = false)
                    )
                }
            }
            
            AlarmScheduler.ALARM_TYPE_HYDRATION -> {
                val requestCode = intent.getIntExtra("requestCode", 0)
                
                // Cancel specific hydration alarm notification
                notificationManager.cancel(requestCode)
                
                // Cancel the specific hydration alarm
                AlarmScheduler.cancelHydrationAlarm(context, requestCode)
                
                // Update hydration config to remove this request code
                val hydrationConfig = com.example.dailyflow.data.PrefsManager.getHydrationConfig(context, username)
                if (hydrationConfig != null) {
                    val updatedRequestCodes = hydrationConfig.requestCodes.filter { it != requestCode }
                    if (updatedRequestCodes.isEmpty()) {
                        // No more hydration alarms, clear the config
                        com.example.dailyflow.data.PrefsManager.setHydrationConfig(
                            context, 
                            username, 
                            com.example.dailyflow.data.PrefsManager.HydrationConfig(0L, 0, emptyList())
                        )
                    } else {
                        // Update with remaining request codes
                        com.example.dailyflow.data.PrefsManager.setHydrationConfig(
                            context, 
                            username, 
                            hydrationConfig.copy(requestCodes = updatedRequestCodes, count = updatedRequestCodes.size)
                        )
                    }
                }
            }
        }
        
        // Close alarm ring activities
        val closeIntent = Intent(context, com.example.dailyflow.ui.alarm.AlarmRingActivity::class.java).apply {
            action = "STOP_ALARM"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(closeIntent)
    }
}
