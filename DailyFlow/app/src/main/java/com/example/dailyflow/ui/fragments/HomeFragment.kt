package com.example.dailyflow.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.MarkerView
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private var habits: List<Pair<String, String>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.home, container, false)

        val pb = v.findViewById<ProgressBar>(R.id.progressDay)
        val tv = v.findViewById<TextView>(R.id.tvPercent)

        val rv = v.findViewById<RecyclerView>(R.id.todayhabits)
        rv.layoutManager = LinearLayoutManager(requireContext())
        
        // Declare adapter variable first
        lateinit var adapter: HabitsAdapter
        
        // Create adapter with callbacks
        adapter = HabitsAdapter(
            onToggle = { id, checked ->
                Storage.setHabitDoneForToday(requireContext(), id, checked)
                updateProgress(pb, tv)
            },
            onDelete = { id ->
                deleteHabit(id, adapter, pb, tv)
            }
        )
        rv.adapter = adapter
        // Load user-defined habits
        habits = Storage.getHabits(requireContext()).map { it.id.toString() to it.name }
        adapter.submit(habits, Storage.getHabitsDoneMapForToday(requireContext()))

        updateProgress(pb, tv)

        // Add habit dialog
        v.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddHabit)
            .setOnClickListener { showAddHabitDialog(adapter, pb, tv) }

        // Mood chart
        val chart = v.findViewById<LineChart>(R.id.chartMood)
        setupMoodChart(chart)
        renderMoodChart(chart)
        return v
    }

    private fun showAddHabitDialog(
        adapter: HabitsAdapter,
        pb: ProgressBar,
        tv: TextView
    ) {
        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_habit, null)
        val et = view.findViewById<android.widget.EditText>(R.id.etHabitName)

        val dialog = android.app.AlertDialog.Builder(ctx)
            .setView(view)
            .create()

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHCancel)
            .setOnClickListener { dialog.dismiss() }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHSave)
            .setOnClickListener {
                val name = et.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    val list = Storage.getHabits(ctx)
                    list.add(Storage.Habit(System.currentTimeMillis(), name))
                    Storage.putHabits(ctx, list)
                    // refresh list and progress
                    habits = list.map { it.id.toString() to it.name }
                    adapter.submit(habits, Storage.getHabitsDoneMapForToday(ctx))
                    updateProgress(pb, tv)
                }
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun deleteHabit(habitId: String, adapter: HabitsAdapter, pb: ProgressBar, tv: TextView) {
        val ctx = requireContext()
        val habitsList = Storage.getHabits(ctx)
        val habitToDelete = habitsList.find { it.id.toString() == habitId }
        
        if (habitToDelete != null) {
            // Remove from habits list
            habitsList.remove(habitToDelete)
            Storage.putHabits(ctx, habitsList)
            
            // Remove from today's completion map
            val todayMap = Storage.getHabitsDoneMapForToday(ctx).toMutableMap()
            todayMap.remove(habitId)
            Storage.setTodayHabits(ctx, todayMap)
            
            // Update UI
            habits = habitsList.map { it.id.toString() to it.name }
            adapter.submit(habits, Storage.getHabitsDoneMapForToday(ctx))
            updateProgress(pb, tv)
        }
    }

    private fun updateProgress(pb: ProgressBar, tv: TextView) {
        val pct = Storage.todayPercent(requireContext())
        pb.progress = pct
        tv.text = "$pct%"
        com.example.dailyflow.widget.HabitWidgetProvider.requestUpdate(requireContext())
    }

    private fun setupMoodChart(chart: LineChart) {
        chart.setNoDataText("No mood data yet")
        chart.description = Description().apply { text = "" }
        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 5f
        chart.axisLeft.granularity = 1f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.legend.apply {
            isEnabled = true
            form = Legend.LegendForm.CIRCLE
            textSize = 12f
        }
    }

    private fun renderMoodChart(chart: LineChart) {
        val ctx = requireContext()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance().apply { time = Date() }
        val labels = mutableListOf<String>()
        val entries = mutableListOf<Entry>()
        for (i in 6 downTo 0) {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = fmt.format(dayCal.time)
            labels += dateStr.substring(5)
            val journal = Storage.getJournalForDate(ctx, dateStr)
            val score = if (journal.isEmpty()) 0f else moodScore(journal.first().mood)
            entries += Entry((6 - i).toFloat(), score)
        }
        val ds = LineDataSet(entries, "Mood score").apply {
            color = resources.getColor(R.color.colorAccent, null)
            setCircleColor(resources.getColor(R.color.colorAccent, null))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillColor = resources.getColor(R.color.colorAccent, null)
            fillAlpha = 40
        }
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.data = LineData(ds)

        // Marker for point values
        chart.marker = MoodMarker(requireContext())
        chart.invalidate()
    }

    private fun moodScore(mood: String): Float = when (mood) {
        "😊" -> 5f
        "😌" -> 4f
        "🙂" -> 3f
        "😕" -> 2f
        "😠" -> 1f
        else -> 0f
    }

    private class MoodMarker(ctx: Context) : MarkerView(ctx, R.layout.marker_mood) {
        private val tv: TextView = findViewById(R.id.tvMarker)
        override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: com.github.mikephil.charting.highlight.Highlight?) {
            tv.text = "Score: ${e?.y?.toInt() ?: 0}"
            super.refreshContent(e, highlight)
        }
        override fun getOffset(): com.github.mikephil.charting.utils.MPPointF {
            return com.github.mikephil.charting.utils.MPPointF(-width / 2f, -height.toFloat())
        }
    }

    private class HabitsAdapter(
        private val onToggle: (String, Boolean) -> Unit,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<HabitVH>() {
        private val items = mutableListOf<Pair<String, String>>()
        private var states: Map<String, Boolean> = emptyMap()
        fun submit(list: List<Pair<String, String>>, state: Map<String, Boolean>) {
            items.clear(); items.addAll(list); states = state; notifyDataSetChanged()
        }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): HabitVH {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_habit, p, false)
            return HabitVH(v)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: HabitVH, pos: Int) {
            val (id, title) = items[pos]
            h.cb.setOnCheckedChangeListener(null)
            h.cb.isChecked = states[id] == true
            h.name.text = title
            h.cb.setOnCheckedChangeListener { _, b -> onToggle(id, b) }
            h.btnDel.setOnClickListener { onDelete(id) }
        }
    }

    private class HabitVH(v: View) : RecyclerView.ViewHolder(v) {
        val cb: CheckBox = v.findViewById(R.id.cbHabitDone)
        val name: TextView = v.findViewById(R.id.HabitName)
        val btnDel: android.widget.ImageButton = v.findViewById(R.id.btnDelete)
    }
}
