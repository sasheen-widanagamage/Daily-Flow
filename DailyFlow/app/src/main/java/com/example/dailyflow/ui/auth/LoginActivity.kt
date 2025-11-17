package com.example.dailyflow.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage
import com.example.dailyflow.data.PrefsManager
import com.example.dailyflow.ui.MainActivity

class LoginActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.login)

        val u = findViewById<EditText>(R.id.edtUsername)
        val p = findViewById<EditText>(R.id.edtPassword)
        findViewById<Button>(R.id.btnloginAccount).setOnClickListener {
            val saved = Storage.getUser(this)
            val ok = saved != null && u.text.toString().trim() == saved.first && p.text.toString() == saved.second
            if (ok) {
                val username = u.text.toString().trim()
                
                // Migrate data to new account
                PrefsManager.migrateFromLegacyStorage(this, username)
                
                // Mark user as onboarded
                Storage.setOnboarded(this, true)
                
                // Set as last active user
                PrefsManager.setLastActiveUsername(this, username)
                
                // Check if permission flow is completed
                if (PrefsManager.isPermissionFlowCompleted(this)) {
                    // Go directly to main activity
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Go to permission gate
                    val intent = Intent(this, PermissionGateActivity::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                }
                finish()
            } else Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.textView3).setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }
}
