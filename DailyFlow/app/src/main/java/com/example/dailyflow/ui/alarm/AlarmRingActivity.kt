package com.example.dailyflow.ui.alarm

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.dailyflow.R
import com.example.dailyflow.databinding.ActivityAlarmRingBinding
import com.example.dailyflow.util.AlarmScheduler
import com.example.dailyflow.util.Feedback


class AlarmRingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAlarmRingBinding
    private var username: String? = null
    private var alarmType: String? = null
    private var requestCode: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle stop action from notification
        if (intent.action == "STOP_ALARM") {
            finish()
            return
        }
        
        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Keep screen on and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        // Extract intent data
        username = intent.getStringExtra("username")
        alarmType = intent.getStringExtra("type")
        requestCode = intent.getIntExtra("requestCode", 0)
        
        val title = intent.getStringExtra("title") ?: "Alarm"
        val message = intent.getStringExtra("message") ?: "Time to wake up!"
        
        // Set up UI
        binding.tvAlarmTitle.text = title
        binding.tvAlarmMessage.text = message
        
        // Set up stop button
        binding.btnStopAlarm.setOnClickListener {
            stopAlarm()
        }
        
        // Start alarm feedback
        username?.let { user ->
            Feedback.startAlarmFeedback(this, user)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop feedback when activity is destroyed
        Feedback.stopAlarmFeedback()
    }
    

    private fun stopAlarm() {
        // Stop feedback
        Feedback.stopAlarmFeedback()
        
        // Dismiss notifications
        val notificationManager = NotificationManagerCompat.from(this)
        
        when (alarmType) {
            AlarmScheduler.ALARM_TYPE_SLEEP -> {
                notificationManager.cancel(424242) // Sleep alarm request code
                
                // Cancel the sleep alarm
                AlarmScheduler.cancelSleepAlarm(this)
                
                // Update sleep config to disabled
                username?.let { user ->
                    val sleepConfig = com.example.dailyflow.data.PrefsManager.getSleepConfig(this, user)
                    if (sleepConfig != null) {
                        com.example.dailyflow.data.PrefsManager.setSleepConfig(
                            this, 
                            user, 
                            sleepConfig.copy(enabled = false)
                        )
                    }
                }
            }
            
            AlarmScheduler.ALARM_TYPE_HYDRATION -> {
                notificationManager.cancel(requestCode)
                
                // Cancel hydration alarm
                AlarmScheduler.cancelHydrationAlarm(this, requestCode)
                
                // Update hydration config to remove this request code
                username?.let { user ->
                    val hydrationConfig = com.example.dailyflow.data.PrefsManager.getHydrationConfig(this, user)
                    if (hydrationConfig != null) {
                        val updatedRequestCodes = hydrationConfig.requestCodes.filter { it != requestCode }
                        if (updatedRequestCodes.isEmpty()) {
                            // No more hydration alarms, clear the config
                            com.example.dailyflow.data.PrefsManager.setHydrationConfig(
                                this, 
                                user, 
                                com.example.dailyflow.data.PrefsManager.HydrationConfig(0L, 0, emptyList())
                            )
                        } else {
                            // Update with remaining request codes
                            com.example.dailyflow.data.PrefsManager.setHydrationConfig(
                                this, 
                                user, 
                                hydrationConfig.copy(requestCodes = updatedRequestCodes, count = updatedRequestCodes.size)
                            )
                        }
                    }
                }
            }
        }

        finish()
    }
}
