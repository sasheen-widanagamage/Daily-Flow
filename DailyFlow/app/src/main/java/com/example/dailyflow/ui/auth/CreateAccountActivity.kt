package com.example.dailyflow.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
import com.example.dailyflow.data.Storage

class CreateAccountActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.createacount)

        val u = findViewById<EditText>(R.id.edtUsername)
        val p = findViewById<EditText>(R.id.edtPassword)
        val c = findViewById<EditText>(R.id.edtConfirmPassword)
        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            val uu = u.text.toString().trim(); val pp = p.text.toString(); val cc = c.text.toString()
            if (uu.isEmpty() || pp.length < 4 || pp != cc) {
                Toast.makeText(this, "Check inputs (min 4 char password)", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            Storage.saveUser(this, uu, pp)
            Storage.setOnboarded(this, true) // Mark user as onboarded
            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
        findViewById<TextView>(R.id.textView3).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }
}
