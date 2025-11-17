package com.example.dailyflow.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyflow.R
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.data.Storage
import com.example.dailyflow.util.AlarmScheduler
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterReminderFragment : Fragment() {

    private lateinit var adapter: UpcomingAdapter
    private var currentUsername: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_water_reminder, container, false)

        // Get current username
        currentUsername = PrefsManager.getLastActiveUsername(requireContext())
        if (currentUsername == null) {
            currentUsername = Storage.getUsername(requireContext())
        }

        // Hours (0..12), default 1
        val npH = v.findViewById<NumberPicker>(R.id.npHours).apply {
            minValue = 0
            maxValue = 12
            value = 1
        }

        // Minutes in 5-min steps (00,05,10,...,55)
        val minuteValues = (0..55 step 5).map { String.format("%02d", it) }.toTypedArray()
        val npM = v.findViewById<NumberPicker>(R.id.npMinutes).apply {
            minValue = 0
            maxValue = minuteValues.size - 1
            displayedValues = minuteValues
            wrapSelectorWheel = false
            value = 0
        }

        // Count (1..12), default 5
        val npC = v.findViewById<NumberPicker>(R.id.npCount).apply {
            minValue = 1
            maxValue = 12
            value = 5
        }

        val tvPrev = v.findViewById<TextView>(R.id.tvPreview)
        fun updatePreview() {
            val mins = npM.value * 5
            tvPrev.text = "Every ${npH.value}h ${mins.toString().padStart(2, '0')}m • ${npC.value} times"
        }
        updatePreview()
        listOf(npH, npM, npC).forEach { it.setOnValueChangedListener { _, _, _ -> updatePreview() } }

        // Upcoming list
        val rv = v.findViewById<RecyclerView>(R.id.rvUpcoming)
        adapter = UpcomingAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        
        // Load existing reminders
        loadExistingReminders()

        v.findViewById<MaterialToolbar>(R.id.topBarBack)
            ?.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        // Actions
        v.findViewById<MaterialButton>(R.id.btnSchedule).setOnClickListener {
            val realMinutes = npM.value * 5
            if (!AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                Toast.makeText(requireContext(), "Exact alarms not allowed. Please enable in Settings.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            schedule(npH.value, realMinutes, npC.value)
        }
        v.findViewById<MaterialButton>(R.id.btnCancelAll).setOnClickListener { cancelAll() }

        return v
    }

    private fun loadExistingReminders() {
        currentUsername?.let { username ->
            val config = PrefsManager.getHydrationConfig(requireContext(), username)
            if (config != null && config.count > 0) {
                val reminders = config.requestCodes.mapIndexed { index, _ ->
                    val triggerTime = System.currentTimeMillis() + (config.intervalMs * (index + 1))
                    Storage.Reminder(triggerTime, config.requestCodes[index])
                }
                adapter.submit(reminders)
            } else {
                adapter.submit(emptyList())
            }
        } ?: run {
            // Fallback to legacy storage
            adapter.submit(Storage.getHydrationReminders(requireContext()))
        }
    }

    private fun schedule(hours: Int, minutes: Int, count: Int) {
        val ctx = requireContext()
        val intervalMs = (hours * 60L + minutes) * 60_000L
        
        currentUsername?.let { username ->
            // Use new alarm system
            val requestCodes = AlarmScheduler.scheduleHydrationBatch(ctx, username, intervalMs, count)
            
            if (requestCodes.isNotEmpty()) {
                // Update the adapter with new reminders
                val reminders = requestCodes.mapIndexed { index, _ ->
                    val triggerTime = System.currentTimeMillis() + (intervalMs * (index + 1))
                    Storage.Reminder(triggerTime, requestCodes[index])
                }
                adapter.submit(reminders)
                Toast.makeText(ctx, "Scheduled ${requestCodes.size} reminders.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, "Failed to schedule reminders. Check exact alarm permissions.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // Fallback to legacy behavior
            val am = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                Toast.makeText(ctx, "Exact alarms not allowed. Please enable in Settings.", Toast.LENGTH_LONG).show()
                return
            }

            val now = System.currentTimeMillis()
            val newList = mutableListOf<Storage.Reminder>()

            repeat(count) { idx ->
                val triggerAt = now + intervalMs * (idx + 1)
                val reqCode = (triggerAt % Int.MAX_VALUE).toInt()

                val pi = android.app.PendingIntent.getBroadcast(
                    ctx,
                    reqCode,
                    android.content.Intent(ctx, com.example.dailyflow.alarm.HydrationReceiver::class.java),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    newList += Storage.Reminder(triggerAt, reqCode)
                } catch (_: SecurityException) {
                    Toast.makeText(ctx, "Exact alarms not permitted on this device.", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            Storage.setHydrationReminders(ctx, newList)
            adapter.submit(newList)
            Toast.makeText(ctx, "Scheduled ${newList.size} reminders.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAll() {
        val ctx = requireContext()
        
        currentUsername?.let { username ->
            // Use new alarm system
            AlarmScheduler.cancelAllHydrationAlarms(ctx, username)
            adapter.submit(emptyList())
            Toast.makeText(ctx, "All reminders cancelled.", Toast.LENGTH_SHORT).show()
        } ?: run {
            // Fallback to legacy behavior
            val am = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val list = Storage.getHydrationReminders(ctx)
            list.forEach {
                val pi = android.app.PendingIntent.getBroadcast(
                    ctx,
                    it.requestCode,
                    android.content.Intent(ctx, com.example.dailyflow.alarm.HydrationReceiver::class.java),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                am.cancel(pi)
            }
            Storage.setHydrationReminders(ctx, emptyList())
            adapter.submit(mutableListOf())
            Toast.makeText(ctx, "All reminders cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Recycler adapter for upcoming times ---
    private class UpcomingAdapter : RecyclerView.Adapter<UpcomingVH>() {
        private val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        private val items = mutableListOf<Storage.Reminder>()
        fun submit(list: List<Storage.Reminder>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): UpcomingVH =
            UpcomingVH(LayoutInflater.from(p.context).inflate(R.layout.item_upcoming_reminder, p, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: UpcomingVH, pos: Int) {
            val r = items[pos]
            h.time.text = fmt.format(Date(r.timeMillis))
            h.idx.text = "#${pos + 1}"
        }
    }

    private class UpcomingVH(v: View) : RecyclerView.ViewHolder(v) {
        val time: TextView = v.findViewById(R.id.tvTime)
        val idx: TextView = v.findViewById(R.id.tvIndex)
    }
}
