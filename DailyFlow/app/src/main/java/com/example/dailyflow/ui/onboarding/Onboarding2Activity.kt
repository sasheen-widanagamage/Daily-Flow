package com.example.dailyflow.ui.onboarding
import android.content.Intent; import android.os.Bundle; import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyflow.R
class Onboarding2Activity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.onboarding2)
        findViewById<ImageView>(R.id.next1).setOnClickListener {
            startActivity(Intent(this, Onboarding3Activity::class.java))
        }
    }
}
