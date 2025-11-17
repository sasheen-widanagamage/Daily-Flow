package com.example.dailyflow.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.ui.auth.LoginActivity
import com.example.dailyflow.ui.MainActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launch_screen)
        
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is already onboarded
            if (Storage.isOnboarded(this)) {
                // User is onboarded, check if they're logged in
                val currentUser = PrefsManager.getLastActiveUsername(this)
                if (currentUser != null) {
                    // User is logged in, go to main activity
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // User is onboarded but not logged in, go to login
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
            else {
                // User is not onboarded, show onboarding
                startActivity(Intent(this, Onboarding1Activity::class.java))
            }
            finish()
        }, 2000) // Reduced from 5000 to 2000ms for better UX
    }
}
