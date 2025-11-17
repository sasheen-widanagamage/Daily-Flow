package com.example.dailyflow.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JournalFragment : Fragment() {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate: String = dateFmt.format(Date())   // must be var
    private var selectedMood: String = "🙂"                      // must be var

    private lateinit var adapter: JAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_journal, container, false)

        val tvDate = v.findViewById<TextView>(R.id.tvSelectedDate)
        val et = v.findViewById<EditText>(R.id.etJournal)
        tvDate.text = "Selected: $selectedDate"

        // Set up back button click listener
        v.findViewById<MaterialToolbar>(R.id.topBarBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        fun bindMoodChip(id: Int, emoji: String) {
            v.findViewById<Chip>(id).setOnClickListener { selectedMood = emoji }
        }
        bindMoodChip(R.id.chipHappy, "😊")
        bindMoodChip(R.id.chipCalm, "😌")
        bindMoodChip(R.id.chipNeutral, "🙂")
        bindMoodChip(R.id.chipSad, "😕")
        bindMoodChip(R.id.chipAngry, "😠")

        val rv = v.findViewById<RecyclerView>(R.id.rvJournalHistory)
        adapter = JAdapter(mutableListOf()) { newList ->
            // persist for the *current selected date*
            Storage.putJournalForDate(requireContext(), selectedDate, newList)
            refresh()
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Calendar date change
        v.findViewById<android.widget.CalendarView>(R.id.calendarView)
            .setOnDateChangeListener { _, year, monthIndex, day ->
                // monthIndex is 0-based
                selectedDate = String.format("%04d-%02d-%02d", year, monthIndex + 1, day)
                tvDate.text = "Selected: $selectedDate"
                refresh()
            }

        // Clear text
        v.findViewById<Button>(R.id.btnClear).setOnClickListener { et.setText("") }

        // Save new entry
        v.findViewById<Button>(R.id.btnSaveUpdate).setOnClickListener {
            val list = Storage.getJournalForDate(requireContext(), selectedDate)
            val text = et.text.toString().trim()
            list.add(Storage.JournalEntry(System.currentTimeMillis(), selectedDate, selectedMood, text))
            Storage.putJournalForDate(requireContext(), selectedDate, list)
            et.setText("")
            refresh()
        }

        refresh()
        return v
    }

    private fun refresh() {
        val list = Storage.getJournalForDate(requireContext(), selectedDate)
        adapter.submit(list)
    }

    // -------- Recycler adapter / view holder ----------
    private class JAdapter(
        private val items: MutableList<Storage.JournalEntry>,
        private val persist: (List<Storage.JournalEntry>) -> Unit
    ) : RecyclerView.Adapter<Jvh>() {

        fun submit(list: List<Storage.JournalEntry>) {
            items.clear()
            items.addAll(list.sortedByDescending { it.id })
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): Jvh {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_journal_entry, p, false)
            return Jvh(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: Jvh, position: Int) {
            val entry = items[position]
            h.emoji.text = entry.mood
            h.date.text = entry.date
            h.snip.text = if (entry.text.length > 80) entry.text.take(80) + "…" else entry.text

            h.btnDel.setOnClickListener {
                val idx = h.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                items.removeAt(idx)
                notifyDataSetChanged()
                persist(items)
            }

            h.btnEdit.setOnClickListener {
                val ctx = h.itemView.context
                val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_journal, null)
                val et = dialogView.findViewById<EditText>(R.id.etJournalDialog)
                et.setText(entry.text)
                var tmpMood = entry.mood

                dialogView.findViewById<Button>(R.id.chipHappy).setOnClickListener { tmpMood = "😊" }
                dialogView.findViewById<Button>(R.id.chipCalm).setOnClickListener { tmpMood = "😌" }
                dialogView.findViewById<Button>(R.id.chipNeutral).setOnClickListener { tmpMood = "🙂" }
                dialogView.findViewById<Button>(R.id.chipSad).setOnClickListener { tmpMood = "😕" }
                dialogView.findViewById<Button>(R.id.chipAngry).setOnClickListener { tmpMood = "😠" }

                val dialog = AlertDialog.Builder(ctx)
                    .setView(dialogView)
                    .create()

                dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
                    .setOnClickListener { dialog.dismiss() }
                dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
                    .setOnClickListener {
                        entry.mood = tmpMood
                        entry.text = et.text.toString()
                        notifyDataSetChanged()
                        persist(items)
                        dialog.dismiss()
                    }

                dialog.show()
            }
        }
    }

    private class Jvh(v: View) : RecyclerView.ViewHolder(v) {
        val emoji: TextView = v.findViewById(R.id.tvMoodEmoji)
        val date: TextView = v.findViewById(R.id.tvEntryDate)
        val snip: TextView = v.findViewById(R.id.tvSnippet)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
        val btnDel: ImageButton = v.findViewById(R.id.btnDelete)
    }
}
