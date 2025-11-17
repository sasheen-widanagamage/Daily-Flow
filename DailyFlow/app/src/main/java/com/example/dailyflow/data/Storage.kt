package com.example.dailyflow.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Storage {

    // ---------- data models ----------
    data class Reminder(val timeMillis: Long, val requestCode: Int)
    data class Sleep(val hour: Int, val minute: Int, val enabled: Boolean, val req: Int)
    data class Exercise(var id: Long, var name: String, var details: String, var done: Boolean = false)
    data class JournalEntry(var id: Long, var date: String, var mood: String, var text: String)
    data class Habit(var id: Long, var name: String)

    // ---------- prefs ----------
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("dailyflow_prefs", Context.MODE_PRIVATE)

    fun clearAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    // ---------- switches: haptics & sound ----------
    fun isVibration(ctx: Context): Boolean = prefs(ctx).getBoolean("vibration", true)
    fun setVibration(ctx: Context, value: Boolean) { prefs(ctx).edit().putBoolean("vibration", value).apply() }

    fun isSound(ctx: Context): Boolean = prefs(ctx).getBoolean("sound", true)
    fun setSound(ctx: Context, value: Boolean) { prefs(ctx).edit().putBoolean("sound", value).apply() }

    // ---------- user credentials  ----------
    fun saveUser(ctx: Context, username: String, password: String) {
        prefs(ctx).edit()
            .putString("user_name", username)
            .putString("user_pass", password)
            .apply()
    }

    fun checkLogin(ctx: Context, username: String, password: String): Boolean {
        val p = prefs(ctx)
        return username == p.getString("user_name", null)
                && password == p.getString("user_pass", null)
    }

    fun getUsername(ctx: Context): String? = prefs(ctx).getString("user_name", null)
    fun getUser(ctx: Context): Pair<String, String>? {
        val p = prefs(ctx)
        val u = p.getString("user_name", null)
        val pw = p.getString("user_pass", null)
        return if (u != null && pw != null) u to pw else null
    }

    // ---------- onboarding flag ----------
    fun setOnboarded(ctx: Context, value: Boolean) { prefs(ctx).edit().putBoolean("onboarded", value).apply() }
    fun isOnboarded(ctx: Context): Boolean = prefs(ctx).getBoolean("onboarded", false)

    // ---------- hydration reminders ----------
    fun setHydrationReminders(ctx: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("time", it.timeMillis)
                put("req", it.requestCode)
            })
        }
        prefs(ctx).edit().putString("hydration_list", arr.toString()).apply()
    }

    fun getHydrationReminders(ctx: Context): MutableList<Reminder> {
        val s = prefs(ctx).getString("hydration_list", "[]") ?: "[]"
        val arr = JSONArray(s)
        val out = mutableListOf<Reminder>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Reminder(
                timeMillis = o.optLong("time", 0L),
                requestCode = o.optInt("req", 0)
            )
        }
        return out
    }

    // ---------- sleep config ----------
    fun setSleep(ctx: Context, cfg: Sleep) {
        val o = JSONObject().apply {
            put("hour", cfg.hour)
            put("minute", cfg.minute)
            put("enabled", cfg.enabled)
            put("req", cfg.req)
        }
        prefs(ctx).edit().putString("sleep_cfg", o.toString()).apply()
    }

    fun getSleep(ctx: Context): Sleep? {
        val raw = prefs(ctx).getString("sleep_cfg", null) ?: return null
        return try {
            val o = JSONObject(raw)
            Sleep(
                hour = o.optInt("hour", 22),
                minute = o.optInt("minute", 0),
                enabled = o.optBoolean("enabled", false),
                req = o.optInt("req", 424242)
            )
        } catch (_: Exception) { null }
    }

    // ---------- exercises ----------
    fun getExercises(ctx: Context): MutableList<Exercise> {
        val s = prefs(ctx).getString("exercises", "[]") ?: "[]"
        val arr = JSONArray(s)
        val list = mutableListOf<Exercise>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += Exercise(
                id = o.optLong("id"),
                name = o.optString("name"),
                details = o.optString("details"),
                done = o.optBoolean("done", false)
            )
        }
        return list
    }

    fun putExercises(ctx: Context, list: List<Exercise>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("details", it.details)
                put("done", it.done)
            })
        }
        prefs(ctx).edit().putString("exercises", arr.toString()).apply()
    }

    // Alias used by UI code
    fun setExercises(ctx: Context, list: List<Exercise>) = putExercises(ctx, list)

    // ---------- habits (user-defined list) ----------
    fun getHabits(ctx: Context): MutableList<Habit> {
        val s = prefs(ctx).getString("habits", "[]") ?: "[]"
        val arr = JSONArray(s)
        val list = mutableListOf<Habit>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += Habit(
                id = o.optLong("id"),
                name = o.optString("name")
            )
        }
        return list
    }

    fun putHabits(ctx: Context, list: List<Habit>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
            })
        }
        prefs(ctx).edit().putString("habits", arr.toString()).apply()
    }

    // ---------- journal per date ----------
    private fun dateKey(date: String) = "journal_$date"

    fun getJournalForDate(ctx: Context, date: String): MutableList<JournalEntry> {
        val s = prefs(ctx).getString(dateKey(date), "[]") ?: "[]"
        val arr = JSONArray(s)
        val list = mutableListOf<JournalEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += JournalEntry(
                id = o.optLong("id"),
                date = o.optString("date", date),
                mood = o.optString("mood", "🙂"),
                text = o.optString("text", "")
            )
        }
        return list
    }

    fun putJournalForDate(ctx: Context, date: String, list: List<JournalEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("date", it.date)
                put("mood", it.mood)
                put("text", it.text)
            })
        }
        prefs(ctx).edit().putString(dateKey(date), arr.toString()).apply()
    }

    // ---------- habits completion for "today"
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun habitsKeyFor(date: String) = "habits_$date"

    fun setHabitDoneForToday(ctx: Context, habitId: String, done: Boolean) {
        val date = today()
        val map = getHabitsDoneMapForDate(ctx, date).toMutableMap()
        map[habitId] = done
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs(ctx).edit().putString(habitsKeyFor(date), obj.toString()).apply()
    }

    fun getHabitsDoneMapForToday(ctx: Context): Map<String, Boolean> =
        getHabitsDoneMapForDate(ctx, today())

    // Convenience for HomeFragment
    fun getTodayHabits(ctx: Context): Map<String, Boolean> = getHabitsDoneMapForToday(ctx)
    fun setTodayHabits(ctx: Context, map: Map<String, Boolean>) {
        val date = today()
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs(ctx).edit().putString(habitsKeyFor(date), obj.toString()).apply()
    }

    private fun getHabitsDoneMapForDate(ctx: Context, date: String): Map<String, Boolean> {
        val raw = prefs(ctx).getString(habitsKeyFor(date), "{}") ?: "{}"
        val o = JSONObject(raw)
        val map = mutableMapOf<String, Boolean>()
        val names = o.keys()
        while (names.hasNext()) {
            val k = names.next()
            map[k] = o.optBoolean(k, false)
        }
        return map
    }

    // Calculate today’s completion percent.
    fun todayPercent(ctx: Context): Int {
        val map = getHabitsDoneMapForToday(ctx)
        if (map.isEmpty()) return 0
        val done = map.values.count { it }
        return ((done * 100.0) / map.size).toInt()
    }

    // ---------- timers
    fun setTimerEnd(ctx: Context, key: String, endAtMillis: Long) {
        prefs(ctx).edit().putLong("timer_end_$key", endAtMillis).apply()
    }

    fun getTimerEnd(ctx: Context, key: String): Long? {
        val v = prefs(ctx).getLong("timer_end_$key", -1L)
        return if (v > 0L) v else null
    }

    fun clearTimerEnd(ctx: Context, key: String) {
        prefs(ctx).edit().remove("timer_end_$key").apply()
    }
}
