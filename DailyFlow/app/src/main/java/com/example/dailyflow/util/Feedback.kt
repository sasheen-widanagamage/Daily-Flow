package com.example.dailyflow.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.dailyflow.data.PrefsManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Feedback {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var executor: ScheduledExecutorService? = null
    private var isPlaying = false
    
    /**
     * Start alarm feedback (sound and vibration) based on user preferences
     */
    fun startAlarmFeedback(context: Context, username: String) {
        if (isPlaying) return
        
        val soundEnabled = PrefsManager.getSoundEnabled(context, username)
        val hapticsEnabled = PrefsManager.getHapticsEnabled(context, username)
        
        if (soundEnabled) {
            startSound(context)
        }
        
        if (hapticsEnabled) {
            startVibration(context)
        }
        
        isPlaying = true
    }
    
    /**
     * Stop all alarm feedback
     */
    fun stopAlarmFeedback() {
        stopSound()
        stopVibration()
        isPlaying = false
    }
    
    /**
     * Check if feedback is currently playing
     */
    fun isFeedbackPlaying(): Boolean = isPlaying
    
    /**
     * Start looping notification sound
     */
    private fun startSound(context: Context) {
        try {
            val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, defaultRingtoneUri)
                
                // Configure audio attributes for notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
                }
                
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to system default if custom ringtone fails
            try {
                val systemRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                systemRingtone?.play()
            } catch (ex: Exception) {
                // Ignore if all sound options fail
            }
        }
    }
    
    /**
     * Stop sound feedback
     */
    private fun stopSound() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Start vibration pattern
     */
    private fun startVibration(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator?.hasVibrator() == true) {
                // Create a repeating vibration pattern
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500) // Wait, vibrate, wait, vibrate, wait, vibrate
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // Repeat from index 0
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0) // Repeat from index 0
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
    
    /**
     * Stop vibration feedback
     */
    private fun stopVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Start a single vibration pulse (for button feedback)
     */
    fun vibrateOnce(context: Context, username: String) {
        if (!PrefsManager.getHapticsEnabled(context, username)) return
        
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
     * Play a single notification sound (for button feedback)
     */
    fun playNotificationSound(context: Context, username: String) {
        if (!PrefsManager.getSoundEnabled(context, username)) return
        
        try {
            val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            ringtone?.play()
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }
    
    /**
     * Clean up resources (call when app is destroyed)
     */
    fun cleanup() {
        stopAlarmFeedback()
        executor?.shutdown()
        executor = null
    }
}
