package com.example.dailyflow.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.dailyflow.R

class HabitsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.habits, container, false)

        fun open(f: Fragment){ requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f).addToBackStack(null).commit() }

        v.findViewById<LinearLayout>(R.id.optDrink).setOnClickListener { open(WaterReminderFragment()) }
        v.findViewById<LinearLayout>(R.id.optExercise).setOnClickListener { open(ExerciseListFragment()) }
        v.findViewById<LinearLayout>(R.id.optSleep).setOnClickListener { open(SleepAlarmFragment()) }
        v.findViewById<LinearLayout>(R.id.optStudy).setOnClickListener { open(StudyTimerFragment()) }
        v.findViewById<LinearLayout>(R.id.optmeditate).setOnClickListener { open(MeditateTimerFragment()) }
        v.findViewById<LinearLayout>(R.id.optJournal).setOnClickListener { open(JournalFragment()) }

        return v
    }
}
