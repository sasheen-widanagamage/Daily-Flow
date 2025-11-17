package com.example.dailyflow.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dailyflow.alarm.AlarmReceiver
import com.example.dailyflow.data.PrefsManager
import java.util.Calendar

/**
 * Handles sleep alarms and hydration reminders with proper permission checks.
 */
object AlarmScheduler {
    
    // Request codes for different alarm types
    private const val SLEEP_ALARM_REQUEST_CODE = 424242
    private const val HYDRATION_ALARM_BASE_CODE = 100000
    
    // Alarm types for the receiver
    const val ALARM_TYPE_SLEEP = "sleep"
    const val ALARM_TYPE_HYDRATION = "hydration"
    
    /**
     * Check if exact alarms are allowed on this device
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * Schedule a daily sleep alarm for the specified time
     */
    fun scheduleDailySleepAlarm(context: Context, username: String, hour: Int, minute: Int): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel existing sleep alarm first
        cancelSleepAlarm(context)
        
        // Calculate next trigger time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("type", ALARM_TYPE_SLEEP)
            putExtra("username", username)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SLEEP_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            
            // Save the configuration
            PrefsManager.setSleepConfig(context, username, PrefsManager.SleepConfig(hour, minute, true))
            true
        } catch (e: SecurityException) {
            false
        }
    }
    
    /**
     * Cancel the sleep alarm
     */
    fun cancelSleepAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SLEEP_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Schedule multiple hydration alarms with the specified interval
     */
    fun scheduleHydrationBatch(
        context: Context, 
        username: String, 
        intervalMs: Long, 
        count: Int
    ): List<Int> {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCodes = mutableListOf<Int>()
        val now = System.currentTimeMillis()
        
        // Cancel existing hydration alarms first
        cancelAllHydrationAlarms(context, username)
        
        repeat(count) { index ->
            val triggerTime = now + (intervalMs * (index + 1))
            val requestCode = HYDRATION_ALARM_BASE_CODE + (triggerTime % Int.MAX_VALUE).toInt()
            
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", ALARM_TYPE_HYDRATION)
                putExtra("username", username)
                putExtra("requestCode", requestCode)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                requestCodes.add(requestCode)
            } catch (e: SecurityException) {
                // Stop scheduling if we hit permission issues
                return requestCodes
            }
        }
        
        // Save the configuration
        if (requestCodes.isNotEmpty()) {
            PrefsManager.setHydrationConfig(
                context, 
                username, 
                PrefsManager.HydrationConfig(intervalMs, count, requestCodes)
            )
        }
        
        return requestCodes
    }
    
    /**
     * Cancel all hydration alarms for a specific user
     */
    fun cancelAllHydrationAlarms(context: Context, username: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val config = PrefsManager.getHydrationConfig(context, username)
        
        config?.requestCodes?.forEach { requestCode ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        
        // Clear the configuration
        PrefsManager.setHydrationConfig(context, username, PrefsManager.HydrationConfig(0L, 0, emptyList()))
    }
    
    /**
     * Cancel a specific hydration alarm by request code
     */
    fun cancelHydrationAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Reschedule all alarms for the current user after boot
     */
    fun rescheduleOnBoot(context: Context, username: String) {
        // Reschedule sleep alarm
        val sleepConfig = PrefsManager.getSleepConfig(context, username)
        if (sleepConfig?.enabled == true) {
            scheduleDailySleepAlarm(context, username, sleepConfig.hour, sleepConfig.minute)
        }
        
        // Reschedule hydration alarms
        val hydrationConfig = PrefsManager.getHydrationConfig(context, username)
        if (hydrationConfig != null && hydrationConfig.count > 0) {
            // Calculate remaining alarms based on current time
            val now = System.currentTimeMillis()
            val remainingCount = hydrationConfig.count - 1
            
            if (remainingCount > 0) {
                scheduleHydrationBatch(context, username, hydrationConfig.intervalMs, remainingCount)
            }
        }
    }
    
    /**
     * Get upcoming alarm times for display
     */
    fun getUpcomingAlarmTimes(context: Context, username: String): List<Long> {
        val times = mutableListOf<Long>()
        
        // Add sleep alarm time
        val sleepConfig = PrefsManager.getSleepConfig(context, username)
        if (sleepConfig?.enabled == true) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, sleepConfig.hour)
                set(Calendar.MINUTE, sleepConfig.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            times.add(calendar.timeInMillis)
        }
        
        // Add hydration alarm times
        val hydrationConfig = PrefsManager.getHydrationConfig(context, username)
        if (hydrationConfig != null) {
            val now = System.currentTimeMillis()
            repeat(hydrationConfig.count) { index ->
                val triggerTime = now + (hydrationConfig.intervalMs * (index + 1))
                times.add(triggerTime)
            }
        }
        
        return times.sorted()
    }
    
    /**
     * Check if any alarms are scheduled for the current user
     */
    fun hasScheduledAlarms(context: Context, username: String): Boolean {
        val sleepConfig = PrefsManager.getSleepConfig(context, username)
        val hydrationConfig = PrefsManager.getHydrationConfig(context, username)
        
        return (sleepConfig?.enabled == true) || (hydrationConfig?.count ?: 0) > 0
    }
}
