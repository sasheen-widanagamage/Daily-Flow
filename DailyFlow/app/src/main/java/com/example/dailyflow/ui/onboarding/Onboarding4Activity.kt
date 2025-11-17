package com.example.dailyflow.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
import com.example.dailyflow.ui.auth.CreateAccountActivity

class Onboarding4Activity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.onboarding4)
        findViewById<Button>(R.id.button).setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
            finish()
        }
    }
}
