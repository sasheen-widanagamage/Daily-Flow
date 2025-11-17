package com.example.dailyflow.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.appbar.MaterialToolbar

class ExerciseListFragment : Fragment() {
    private lateinit var adapter: ExAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_exercise_list, container, false)
        val rv = v.findViewById<RecyclerView>(R.id.rvExercises)
        adapter = ExAdapter(mutableListOf()) { list -> Storage.setExercises(requireContext(), list) }
        rv.layoutManager = LinearLayoutManager(requireContext()); rv.adapter = adapter
        adapter.submit(Storage.getExercises(requireContext()))

        v.findViewById<FloatingActionButton>(R.id.fabAddExercise).setOnClickListener { showDialog() }
        v.findViewById<MaterialToolbar>(R.id.topBarBack)
            ?.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        return v
    }
    private fun showDialog(edit: Storage.Exercise? = null) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_exercise, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etDet = view.findViewById<EditText>(R.id.etDetails)
        if (edit != null) { etName.setText(edit.name); etDet.setText(edit.details) }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
            .setOnClickListener {
                val list = adapter.items
                if (edit == null) list.add(Storage.Exercise(System.currentTimeMillis(), etName.text.toString(), etDet.text.toString()))
                else { edit.name = etName.text.toString(); edit.details = etDet.text.toString() }
                adapter.notifyDataSetChanged()
                Storage.setExercises(requireContext(), list)
                dialog.dismiss()
            }

        dialog.show()
    }

    private inner class ExAdapter(val items: MutableList<Storage.Exercise>, val onPersist:(List<Storage.Exercise>)->Unit)
        : RecyclerView.Adapter<ExVH>() {
        fun submit(list: MutableList<Storage.Exercise>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
            ExVH(LayoutInflater.from(p.context).inflate(R.layout.item_exercise, p, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: ExVH, pos: Int) {
            val e = items[pos]
            h.name.text = e.name; h.det.text = e.details; h.cb.isChecked = e.done
            h.cb.setOnCheckedChangeListener { _, b -> e.done = b; onPersist(items) }
            h.btnEdit.setOnClickListener { showDialog(e) }
            h.btnDel.setOnClickListener { items.removeAt(pos); notifyDataSetChanged(); onPersist(items) }
        }
    }
    private class ExVH(v: View): RecyclerView.ViewHolder(v) {
        val cb: CheckBox = v.findViewById(R.id.cbDone)
        val name: TextView = v.findViewById(R.id.tvExerciseName)
        val det: TextView = v.findViewById(R.id.tvExerciseDetails)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
        val btnDel: ImageButton = v.findViewById(R.id.btnDelete)
    }
}
