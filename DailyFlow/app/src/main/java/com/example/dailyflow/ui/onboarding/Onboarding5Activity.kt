package com.example.dailyflow.ui.onboarding
import android.content.Intent; import android.os.Bundle; import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
class Onboarding5Activity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.onboarding5)
        findViewById<ImageView>(R.id.next1).setOnClickListener {
            startActivity(Intent(this, Onboarding6Activity::class.java))
        }
    }
}
