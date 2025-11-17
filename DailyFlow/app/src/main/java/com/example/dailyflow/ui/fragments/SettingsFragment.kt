package com.example.dailyflow.ui.fragments

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dailyflow.R
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.data.Storage
import com.example.dailyflow.util.AlarmScheduler
import com.example.dailyflow.util.Permissions
import com.example.dailyflow.widget.HabitWidgetProvider
import com.example.dailyflow.ui.more.PrivacyActivity
import com.example.dailyflow.util.NotificationHelper

class SettingsFragment : Fragment() {

    private var currentUsername: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current username
        currentUsername = PrefsManager.getLastActiveUsername(requireContext())
        if (currentUsername == null) {
            // Fallback to legacy storage
            currentUsername = Storage.getUsername(requireContext())
        }

        val btnC = view.findViewById<Button>(R.id.btnClear)
        val btnNotifications = view.findViewById<TextView>(R.id.btnNotifications)
        val btnAlarms = view.findViewById<TextView>(R.id.btnAlarms)
        val tvAbout = view.findViewById<TextView>(R.id.tvAbout)
        val tvPrivacy = view.findViewById<TextView>(R.id.tvPrivacy)

        // Set up about section
        try {
            val pInfo = requireContext()
                .packageManager
                .getPackageInfo(requireContext().packageName, 0)
            tvAbout.text = "About: DailyFlow v${pInfo.versionName}"
        } catch (e: Exception) {
            tvAbout.text = "About: DailyFlow v1.0"
            android.util.Log.e("SettingsFragment", "Error getting package info", e)
        }

        tvAbout.setOnClickListener {
            // Navigate to About Us fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AboutUsFragment())
                .addToBackStack(null)
                .commit()
        }
        tvPrivacy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyActivity::class.java))
        }

        // Set up management buttons
        btnNotifications.setOnClickListener {
            openNotificationSettings()
        }

        btnAlarms.setOnClickListener {
            openAlarmSettings()
        }

        // Set up clear data button
        btnC.setOnClickListener {
            clearAllData()
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general app settings if notification settings are not available
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                NotificationHelper.showToast(requireContext(), "Unable to open notification settings", currentUsername)
                android.util.Log.e("SettingsFragment", "Error opening notification settings", e2)
            }
        }
    }

    private fun openAlarmSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            } else {
                // Fallback to general app settings for older Android versions
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            NotificationHelper.showToast(requireContext(), "Unable to open alarm settings", currentUsername)
            android.util.Log.e("SettingsFragment", "Error opening alarm settings", e)
        }
    }

    private fun clearAllData() {
        val ctx = requireContext()
        
        try {
            currentUsername?.let { username ->
                // Cancel all alarms for this user
                AlarmScheduler.cancelSleepAlarm(ctx)
                AlarmScheduler.cancelAllHydrationAlarms(ctx, username)

                // Clear per-account data (preferences)
                PrefsManager.clearAccountData(ctx, username)

                // Also clear legacy storage so journals/exercises/habits and daily maps are removed
                Storage.clearAll(ctx)

                // Reset permission flow so it appears on next login
                PrefsManager.setPermissionFlowCompleted(ctx, false)

                // Refresh any widgets
                HabitWidgetProvider.requestUpdate(ctx)

                Toast.makeText(ctx, "All data cleared for $username", Toast.LENGTH_SHORT).show()
            } ?: run {
            // Fallback to legacy clearing
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Cancel all hydration alarms
            Storage.getHydrationReminders(ctx).forEach {
                val pi = android.app.PendingIntent.getBroadcast(
                    ctx,
                    it.requestCode,
                    Intent(ctx, com.example.dailyflow.alarm.HydrationReceiver::class.java),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                am.cancel(pi)
            }

            // Cancel sleep alarm
            val sleepPi = android.app.PendingIntent.getBroadcast(
                ctx,
                424242,
                Intent(ctx, com.example.dailyflow.alarm.SleepAlarmReceiver::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(sleepPi)

            // Clear prefs + refresh widget
            Storage.clearAll(ctx)
            HabitWidgetProvider.requestUpdate(ctx)
            Toast.makeText(ctx, "All data & alarms cleared", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error clearing data: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("SettingsFragment", "Error clearing data", e)
        }
    }
}
