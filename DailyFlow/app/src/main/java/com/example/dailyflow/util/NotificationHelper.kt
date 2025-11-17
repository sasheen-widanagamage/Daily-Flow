package com.example.dailyflow.util

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.data.Storage

/**
 * Centralized helper for all notifications, sounds, and vibrations in the app.
 */
object NotificationHelper {
    
    /**
     * Show a toast message with optional sound/vibration based on user settings
     */
    fun showToast(context: Context, message: String, username: String? = null, withFeedback: Boolean = false) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        
        if (withFeedback) {
            // Get current user or use legacy settings
            val currentUser = username ?: PrefsManager.getLastActiveUsername(context) ?: Storage.getUsername(context)
            
            if (currentUser != null) {
                // Use per-user settings
                if (PrefsManager.getHapticsEnabled(context, currentUser)) {
                    vibrateOnce(context)
                }
                if (PrefsManager.getSoundEnabled(context, currentUser)) {
                    playNotificationSound(context)
                }
            } else {
                // Use legacy settings
                if (Storage.isVibration(context)) {
                    vibrateOnce(context)
                }
                if (Storage.isSound(context)) {
                    playNotificationSound(context)
                }
            }
        }
    }
    
    /**
     * Create a notification builder with sound/vibration settings applied
     */
    fun createNotificationBuilder(
        context: Context, 
        channelId: String, 
        username: String? = null
    ): NotificationCompat.Builder {
        val currentUser = username ?: PrefsManager.getLastActiveUsername(context) ?: Storage.getUsername(context)
        val soundEnabled = if (currentUser != null) {
            PrefsManager.getSoundEnabled(context, currentUser)
        } else {
            Storage.isSound(context)
        }
        
        return NotificationCompat.Builder(context, channelId)
            .setSilent(!soundEnabled)
    }
    
    /**
     * Play notification sound if enabled
     */
    fun playNotificationSound(context: Context, username: String? = null) {
        val currentUser = username ?: PrefsManager.getLastActiveUsername(context) ?: Storage.getUsername(context)
        val soundEnabled = if (currentUser != null) {
            PrefsManager.getSoundEnabled(context, currentUser)
        } else {
            Storage.isSound(context)
        }
        
        if (!soundEnabled) return
        
        try {
            val ringtone = RingtoneManager.getRingtone(
                context, 
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            ringtone?.play()
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }
    
    /**
     * Vibrate once if enabled
     */
    fun vibrateOnce(context: Context, username: String? = null) {
        val currentUser = username ?: PrefsManager.getLastActiveUsername(context) ?: Storage.getUsername(context)
        val hapticsEnabled = if (currentUser != null) {
            PrefsManager.getHapticsEnabled(context, currentUser)
        } else {
            Storage.isVibration(context)
        }
        
        if (!hapticsEnabled) return
        
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
    
    /**
     * Check if sound is enabled
     */
    fun isSoundEnabled(context: Context, username: String? = null): Boolean {
        val currentUser = username ?: PrefsManager.getLastActiveUsername(context) ?: Storage.getUsername(context)
        return if (currentUser != null) {
            PrefsManager.getSoundEnabled(context, currentUser)
        } else {
            Storage.isSound(context)
        }
    }
    
    /**
     * Check if vibration is enabled
     */
    fun isVibrationEnabled(context: Context, username: String? = null): Boolean {
        val currentUser = username ?: PrefsManager.getLastActiveUsername(context) ?: Storage.getUsername(context)
        return if (currentUser != null) {
            PrefsManager.getHapticsEnabled(context, currentUser)
        } else {
            Storage.isVibration(context)
        }
    }
}
