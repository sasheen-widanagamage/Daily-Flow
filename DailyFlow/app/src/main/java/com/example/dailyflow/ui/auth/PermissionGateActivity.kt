package com.example.dailyflow.ui.auth

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dailyflow.R
import com.example.dailyflow.databinding.ActivityPermissionGateBinding
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.ui.MainActivity
import com.example.dailyflow.util.AlarmScheduler


class PermissionGateActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPermissionGateBinding
    private var username: String? = null
    private var notificationsGranted = false
    private var exactAlarmsAllowed = false
    
    // Permission launcher for POST_NOTIFICATIONS
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsGranted = isGranted
        updateNotificationCard()
        updateContinueButton()
    }
    
    // Activity launcher for exact alarm settings
    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-check exact alarm permission when returning from settings
        checkExactAlarmPermission()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityPermissionGateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get username from intent
        username = intent.getStringExtra("username")
        if (username == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        // Set up UI
        setupUI()
        
        // Check current permission states
        checkNotificationPermission()
        checkExactAlarmPermission()
        
        // Set up continue button
        binding.btnContinue.setOnClickListener {
            if (canContinue()) {
                completePermissionFlow()
            }
        }
        
        // Set up skip button
        binding.btnSkip.setOnClickListener {
            skipPermissionFlow()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from settings
        checkNotificationPermission()
        checkExactAlarmPermission()
    }
    
    private fun setupUI() {
        // Set up notification permission card
        binding.cardNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                if (!notificationsGranted) {
                    requestNotificationPermission()
                } else {
                    openNotificationSettings()
                }
            }
        }
        
        // Set up exact alarm permission card
        binding.cardExactAlarms.setOnClickListener {
            if (!exactAlarmsAllowed) {
                requestExactAlarmPermission()
            }
        }
        
        // Set up sound toggle
        binding.swSound.isChecked = PrefsManager.getSoundEnabled(this, username!!)
        binding.swSound.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setSoundEnabled(this, username!!, isChecked)
        }
        
        // Set up vibration toggle
        binding.swVibrate.isChecked = PrefsManager.getHapticsEnabled(this, username!!)
        binding.swVibrate.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setHapticsEnabled(this, username!!, isChecked)
        }
    }
    
    private fun checkNotificationPermission() {
        notificationsGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        updateNotificationCard()
        updateContinueButton()
    }
    
    private fun checkExactAlarmPermission() {
        exactAlarmsAllowed = AlarmScheduler.canScheduleExactAlarms(this)
        updateExactAlarmCard()
        updateContinueButton()
    }
    
    private fun updateNotificationCard() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (notificationsGranted) {
                binding.tvNotificationStatus.text = "✓ Granted"
                binding.tvNotificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            } else {
                binding.tvNotificationStatus.text = "✗ Denied"
                binding.tvNotificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        } else {
            binding.tvNotificationStatus.text = "Not Required"
            binding.tvNotificationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }
    
    private fun updateExactAlarmCard() {
        if (exactAlarmsAllowed) {
            binding.tvExactAlarmStatus.text = "✓ Allowed"
            binding.tvExactAlarmStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.tvExactAlarmStatus.text = "✗ Not Allowed"
            binding.tvExactAlarmStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
    
    private fun updateContinueButton() {
        binding.btnContinue.isEnabled = canContinue()
    }
    
    private fun canContinue(): Boolean {
        // Can continue if:
        // 1. API < 33 (notifications not required) OR notifications granted
        // 2. Exact alarms allowed OR user can skip
        val notificationsOk = Build.VERSION.SDK_INT < 33 || notificationsGranted
        val exactAlarmsOk = exactAlarmsAllowed || binding.btnSkip.visibility == android.view.View.VISIBLE
        
        return notificationsOk && exactAlarmsOk
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
    
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            exactAlarmLauncher.launch(intent)
        }
    }
    
    private fun completePermissionFlow() {
        // Mark permission flow as completed
        PrefsManager.setPermissionFlowCompleted(this, true)
        PrefsManager.setLastActiveUsername(this, username!!)
        
        // Go to main activity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    private fun skipPermissionFlow() {
        // Mark exact alarm as skipped
        PrefsManager.setExactAlarmSkipped(this, username!!, true)
        
        // Complete the flow
        completePermissionFlow()
        
        Toast.makeText(this, "You can enable exact alarms later in Settings", Toast.LENGTH_LONG).show()
    }
}
