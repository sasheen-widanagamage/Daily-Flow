package com.example.dailyflow.ui.fragments

import android.os.Build
import android.os.Bundle
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dailyflow.R
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.data.Storage
import com.example.dailyflow.util.AlarmScheduler
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SleepAlarmFragment : Fragment(R.layout.fragment_sleep_alarm) {

    private var currentUsername: String? = null

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current username
        currentUsername = PrefsManager.getLastActiveUsername(requireContext())
        if (currentUsername == null) {
            currentUsername = Storage.getUsername(requireContext())
        }

        view.findViewById<MaterialToolbar>(R.id.topBarBack)
            ?.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val tp = view.findViewById<TimePicker>(R.id.timePicker)
        val sw = view.findViewById<SwitchMaterial>(R.id.swEnabled)
        val btn = view.findViewById<MaterialButton>(R.id.btnSaveUpdate)

        // Set 12-hour format
        tp.setIs24HourView(false)

        // Load saved config
        loadSavedConfig(tp, sw)

        btn.setOnClickListener {
            val hour = if (Build.VERSION.SDK_INT >= 23) tp.hour else tp.currentHour
            val minute = if (Build.VERSION.SDK_INT >= 23) tp.minute else tp.currentMinute
            val enabled = sw.isChecked
            
            if (enabled && !AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                Toast.makeText(requireContext(), "Exact alarms not allowed. Please enable in Settings.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            saveAndSchedule(hour, minute, enabled)
        }
    }

    private fun loadSavedConfig(timePicker: TimePicker, switch: SwitchMaterial) {
        currentUsername?.let { username ->
            // Load from new per-account preferences
            val config = PrefsManager.getSleepConfig(requireContext(), username)
            if (config != null) {
                switch.isChecked = config.enabled
                if (Build.VERSION.SDK_INT >= 23) {
                    timePicker.hour = config.hour
                    timePicker.minute = config.minute
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        timePicker.currentHour = config.hour
                        timePicker.currentMinute = config.minute
                    }
                }
            }
        } ?: run {
            // Fallback to legacy storage
            Storage.getSleep(requireContext())?.let { cfg ->
                switch.isChecked = cfg.enabled
                if (Build.VERSION.SDK_INT >= 23) {
                    timePicker.hour = cfg.hour
                    timePicker.minute = cfg.minute
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        timePicker.currentHour = cfg.hour
                        timePicker.currentMinute = cfg.minute
                    }
                }
            }
        }
    }

    private fun saveAndSchedule(hour: Int, minute: Int, enabled: Boolean) {
        val ctx = requireContext()
        
        currentUsername?.let { username ->
            if (enabled) {
                // Schedule exact alarm
                val success = AlarmScheduler.scheduleDailySleepAlarm(ctx, username, hour, minute)
                if (success) {
                    Toast.makeText(ctx, "Sleep alarm set for %02d:%02d".format(hour, minute), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Failed to set alarm. Check exact alarm permissions.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Cancel alarm
                AlarmScheduler.cancelSleepAlarm(ctx)
                PrefsManager.setSleepConfig(ctx, username, PrefsManager.SleepConfig(hour, minute, false))
                Toast.makeText(ctx, "Sleep alarm disabled", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // Fallback to legacy behavior
            val am = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val pi = android.app.PendingIntent.getBroadcast(
                ctx,
                424242,
                android.content.Intent(ctx, com.example.dailyflow.alarm.SleepAlarmReceiver::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)

            if (enabled) {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    if (before(java.util.Calendar.getInstance())) {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                }

                am.setRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    android.app.AlarmManager.INTERVAL_DAY,
                    pi
                )
            }

            Storage.setSleep(ctx, Storage.Sleep(hour, minute, enabled, req = 424242))
            Toast.makeText(
                ctx,
                if (enabled) "Bedtime saved: %02d:%02d".format(hour, minute) else "Bedtime alarm disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
