package com.example.dailyflow.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.dailyflow.R
import com.google.android.material.appbar.MaterialToolbar

class AboutUsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.about_us, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up back button click listener
        view.findViewById<MaterialToolbar>(R.id.topBarBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}

