package com.example.dailyflow.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


object Permissions {
    private const val REQ_NOTI = 1001

    fun hasPostNotifications(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }


    fun ensureNotifications(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTI
                )
            }
        }
    }

    fun ensureExactAlarmOrToast(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= 31) {
            if (!hasExactAlarm(activity)) {
                requestExactAlarm(activity)
            }
        }
    }

    fun hasExactAlarm(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else true
    }

    fun requestExactAlarm(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            i.data = Uri.parse("package:${ctx.packageName}")
            ctx.startActivity(i)
        }
    }
}
