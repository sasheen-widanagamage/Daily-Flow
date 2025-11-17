package com.example.dailyflow.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailyflow.util.AlarmScheduler
import com.example.dailyflow.data.PrefsManager

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            // Get the last active username
            val username = PrefsManager.getLastActiveUsername(context)
            
            if (username != null) {
                // Reschedule all alarms for the current user
                AlarmScheduler.rescheduleOnBoot(context, username)
            }
        }
    }
}
