package com.example.dailyflow.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages per-account SharedPreferences
 */
object PrefsManager {
    
    // Global app preferences (shared across all accounts)
    private const val GLOBAL_PREFS_NAME = "app_prefs"
    
    // Per-account preferences key prefix
    private const val USER_PREFS_PREFIX = "user_"
    
    // Global preference keys
    private const val KEY_PERM_FLOW_COMPLETED = "perm_flow_completed"
    private const val KEY_LAST_ACTIVE_USERNAME = "last_active_username"
    
    // Per-account preference keys
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_SLEEP_TIME_HOURS = "sleep_time_hours"
    private const val KEY_SLEEP_TIME_MINUTES = "sleep_time_minutes"
    private const val KEY_SLEEP_ENABLED = "sleep_enabled"
    private const val KEY_HYDRATION_REQ_CODES = "hydration_req_codes"
    private const val KEY_HYDRATION_INTERVAL_MS = "hydration_interval_ms"
    private const val KEY_EXACT_ALARM_SKIPPED = "exact_alarm_skipped"
    
    // Data model classes
    data class Reminder(val timeMillis: Long, val requestCode: Int)
    data class SleepConfig(val hour: Int, val minute: Int, val enabled: Boolean)
    data class HydrationConfig(val intervalMs: Long, val count: Int, val requestCodes: List<Int>)
    
    // Global preferences
    private fun getGlobalPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Per-account preferences
    private fun getUserPrefs(context: Context, username: String): SharedPreferences {
        val prefsName = "$USER_PREFS_PREFIX$username"
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    
    // Global preference methods
    fun isPermissionFlowCompleted(context: Context): Boolean {
        return getGlobalPrefs(context).getBoolean(KEY_PERM_FLOW_COMPLETED, false)
    }
    
    fun setPermissionFlowCompleted(context: Context, completed: Boolean) {
        getGlobalPrefs(context).edit().putBoolean(KEY_PERM_FLOW_COMPLETED, completed).apply()
    }
    
    fun getLastActiveUsername(context: Context): String? {
        return getGlobalPrefs(context).getString(KEY_LAST_ACTIVE_USERNAME, null)
    }
    
    fun setLastActiveUsername(context: Context, username: String) {
        getGlobalPrefs(context).edit().putString(KEY_LAST_ACTIVE_USERNAME, username).apply()
    }
    
    // Per-account preference methods
    fun getSoundEnabled(context: Context, username: String): Boolean {
        return getUserPrefs(context, username).getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    fun setSoundEnabled(context: Context, username: String, enabled: Boolean) {
        getUserPrefs(context, username).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    fun getHapticsEnabled(context: Context, username: String): Boolean {
        return getUserPrefs(context, username).getBoolean(KEY_HAPTICS_ENABLED, true)
    }
    
    fun setHapticsEnabled(context: Context, username: String, enabled: Boolean) {
        getUserPrefs(context, username).edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }
    
    fun getSleepConfig(context: Context, username: String): SleepConfig? {
        val prefs = getUserPrefs(context, username)
        val hour = prefs.getInt(KEY_SLEEP_TIME_HOURS, -1)
        val minute = prefs.getInt(KEY_SLEEP_TIME_MINUTES, -1)
        val enabled = prefs.getBoolean(KEY_SLEEP_ENABLED, false)
        
        return if (hour >= 0 && minute >= 0) {
            SleepConfig(hour, minute, enabled)
        } else null
    }
    
    fun setSleepConfig(context: Context, username: String, config: SleepConfig) {
        getUserPrefs(context, username).edit()
            .putInt(KEY_SLEEP_TIME_HOURS, config.hour)
            .putInt(KEY_SLEEP_TIME_MINUTES, config.minute)
            .putBoolean(KEY_SLEEP_ENABLED, config.enabled)
            .apply()
    }
    
    fun getHydrationConfig(context: Context, username: String): HydrationConfig? {
        val prefs = getUserPrefs(context, username)
        val intervalMs = prefs.getLong(KEY_HYDRATION_INTERVAL_MS, -1L)
        val requestCodesJson = prefs.getString(KEY_HYDRATION_REQ_CODES, "[]") ?: "[]"
        
        return if (intervalMs > 0) {
            val requestCodes = try {
                val jsonArray = JSONArray(requestCodesJson)
                val codes = mutableListOf<Int>()
                for (i in 0 until jsonArray.length()) {
                    codes.add(jsonArray.getInt(i))
                }
                codes
            } catch (e: Exception) {
                emptyList()
            }
            HydrationConfig(intervalMs, requestCodes.size, requestCodes)
        } else null
    }
    
    fun setHydrationConfig(context: Context, username: String, config: HydrationConfig) {
        val requestCodesJson = JSONArray().apply {
            config.requestCodes.forEach { put(it) }
        }.toString()
        
        getUserPrefs(context, username).edit()
            .putLong(KEY_HYDRATION_INTERVAL_MS, config.intervalMs)
            .putString(KEY_HYDRATION_REQ_CODES, requestCodesJson)
            .apply()
    }
    
    fun isExactAlarmSkipped(context: Context, username: String): Boolean {
        return getUserPrefs(context, username).getBoolean(KEY_EXACT_ALARM_SKIPPED, false)
    }
    
    fun setExactAlarmSkipped(context: Context, username: String, skipped: Boolean) {
        getUserPrefs(context, username).edit().putBoolean(KEY_EXACT_ALARM_SKIPPED, skipped).apply()
    }
    
    // Clear all data
    fun clearAccountData(context: Context, username: String) {
        getUserPrefs(context, username).edit().clear().apply()
        // Reset permission flow for this account
        setPermissionFlowCompleted(context, false)
    }
    
    // Clear all data
    fun clearAllData(context: Context) {
        // Clear global prefs
        getGlobalPrefs(context).edit().clear().apply()
        
        // Clear all user prefs files
        val prefsDir = context.filesDir.parent + "/shared_prefs/"
        val prefsFiles = java.io.File(prefsDir).listFiles { file ->
            file.name.startsWith("$USER_PREFS_PREFIX") && file.name.endsWith(".xml")
        }
        prefsFiles?.forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {

            }
        }
    }
    
    // Get all usernames that have preferences
    fun getAllUsernames(context: Context): List<String> {
        val prefsDir = context.filesDir.parent + "/shared_prefs/"
        val prefsFiles = java.io.File(prefsDir).listFiles { file ->
            file.name.startsWith("$USER_PREFS_PREFIX") && file.name.endsWith(".xml")
        }
        
        return prefsFiles?.mapNotNull { file ->
            try {
                val name = file.name.removePrefix("$USER_PREFS_PREFIX").removeSuffix(".xml")
                name
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }
    
    // Legacy compatibility methods for existing Storage class
    fun migrateFromLegacyStorage(context: Context, username: String) {
        val legacyPrefs = context.getSharedPreferences("dailyflow_prefs", Context.MODE_PRIVATE)
        val userPrefs = getUserPrefs(context, username)
        
        // Migrate sound and vibration settings
        val soundEnabled = legacyPrefs.getBoolean("sound", true)
        val vibrationEnabled = legacyPrefs.getBoolean("vibration", true)
        
        userPrefs.edit()
            .putBoolean(KEY_SOUND_ENABLED, soundEnabled)
            .putBoolean(KEY_HAPTICS_ENABLED, vibrationEnabled)
            .apply()
        
        // Migrate sleep config
        val sleepJson = legacyPrefs.getString("sleep_cfg", null)
        if (sleepJson != null) {
            try {
                val sleepObj = JSONObject(sleepJson)
                val hour = sleepObj.optInt("hour", 22)
                val minute = sleepObj.optInt("minute", 0)
                val enabled = sleepObj.optBoolean("enabled", false)
                
                setSleepConfig(context, username, SleepConfig(hour, minute, enabled))
            } catch (e: Exception) {
                // Ignore migration errors
            }
        }
        
        // Migrate hydration reminders
        val hydrationJson = legacyPrefs.getString("hydration_list", "[]")
        if (hydrationJson != null && hydrationJson != "[]") {
            try {
                val hydrationArray = JSONArray(hydrationJson)
                val requestCodes = mutableListOf<Int>()
                for (i in 0 until hydrationArray.length()) {
                    val reminder = hydrationArray.getJSONObject(i)
                    requestCodes.add(reminder.optInt("req", 0))
                }
                
                if (requestCodes.isNotEmpty()) {
                    // Calculate interval from first two reminders if available
                    val intervalMs = if (hydrationArray.length() >= 2) {
                        val first = hydrationArray.getJSONObject(0).optLong("time", 0L)
                        val second = hydrationArray.getJSONObject(1).optLong("time", 0L)
                        second - first
                    } else {
                        3600000L // Default 1 hour
                    }
                    
                    setHydrationConfig(context, username, HydrationConfig(intervalMs, requestCodes.size, requestCodes))
                }
            } catch (e: Exception) {
                // Ignore migration errors
            }
        }
    }
}
