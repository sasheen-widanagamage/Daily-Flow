package com.example.dailyflow.ui.fragments

import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.dailyflow.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar

class MeditateTimerFragment: Fragment() {
    private var timer: CountDownTimer? = null
    private var total = 0L
    private var left = 0L
    private val key = "meditate"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_meditate_timer, container, false)
        v.findViewById<MaterialToolbar>(R.id.topBarBack)
            ?.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        val npH = v.findViewById<NumberPicker>(R.id.npHours1).apply { minValue=0; maxValue=5 }
        val npM = v.findViewById<NumberPicker>(R.id.npMinutes1).apply { minValue=0; maxValue=59; value=10 }
        val tv = v.findViewById<TextView>(R.id.tvCountdown1)
        val pb = v.findViewById<ProgressBar>(R.id.progressTimer1)

        fun setTime(h:Int,m:Int){ total=((h*60+m)*60*1000L); left=total; tv.text=format(left); pb.progress=0 }
        setTime(npH.value, npM.value)
        listOf(npH,npM).forEach { it.setOnValueChangedListener { _,_,_ -> setTime(npH.value, npM.value) } }

        fun start() {
            timer?.cancel()
            val endAt = System.currentTimeMillis() + left
            com.example.dailyflow.data.Storage.setTimerEnd(requireContext(), key, endAt)
            scheduleAlarm(endAt, "Meditation timer")
            timer = object: CountDownTimer(left, 1000) {
                override fun onTick(ms: Long) { left = ms; tv.text = format(ms); pb.progress = if(total>0) ((total-ms)*100/total).toInt() else 0 }
                override fun onFinish() {
                    tv.text = "00:00:00"; pb.progress = 100
                    com.example.dailyflow.data.Storage.clearTimerEnd(requireContext(), key)
                    try { RingtoneManager.getRingtone(requireContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play() } catch(_:Exception){}
                }
            }.start()
        }
        v.findViewById<MaterialButton>(R.id.btnStart1).setOnClickListener { if (total>0) start() }
        v.findViewById<MaterialButton>(R.id.btnPause1).setOnClickListener { timer?.cancel(); cancelAlarm() }
        v.findViewById<MaterialButton>(R.id.btnReset1).setOnClickListener { timer?.cancel(); cancelAlarm(); setTime(npH.value, npM.value); com.example.dailyflow.data.Storage.clearTimerEnd(requireContext(), key) }
        com.example.dailyflow.data.Storage.getTimerEnd(requireContext(), key)?.let { endAt ->
            val remaining = endAt - System.currentTimeMillis()
            if (remaining > 0) { left = remaining; total = remaining; tv.text = format(left) }
        }
        return v
    }
    private fun scheduleAlarm(endAt: Long, name: String) {
        val ctx = requireContext()
        val am = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            ctx, 2202,
            android.content.Intent(ctx, com.example.dailyflow.alarm.TimerReceiver::class.java).putExtra("name", name),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, endAt, pi)
    }
    private fun cancelAlarm() {
        val ctx = requireContext()
        val am = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            ctx, 2202,
            android.content.Intent(ctx, com.example.dailyflow.alarm.TimerReceiver::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
    private fun format(ms: Long): String {
        val s = ms/1000; val h=s/3600; val m=(s%3600)/60; val ss=s%60
        return "%02d:%02d:%02d".format(h,m,ss)
    }
}
