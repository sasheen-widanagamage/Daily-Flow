package com.example.dailyflow.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
import com.example.dailyflow.databinding.ActivityMainBinding
import com.example.dailyflow.ui.fragments.*

class MainActivity : AppCompatActivity() {
    private lateinit var vb: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // default
        load(HomeFragment())

        vb.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> load(HomeFragment())
                R.id.nav_habits -> load(HabitsFragment())
                R.id.nav_settings -> load(SettingsFragment())
            }
            true
        }
    }
    private fun load(f: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f).commit()
    }
}
