package com.example.dailyflow.ui.more

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
import com.google.android.material.appbar.MaterialToolbar

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.privacy_policy)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<MaterialToolbar>(R.id.topBarBack)
            ?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}


