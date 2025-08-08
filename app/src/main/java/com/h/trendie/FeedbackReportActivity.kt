// FeedbackReportActivity.kt
package com.h.trendie

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.h.trendie.data.AppDatabase
import com.h.trendie.data.FeedbackHistory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FeedbackReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_report)

        val videoTitle = intent.getStringExtra("videoTitle") ?: "제목 없음"
        val nickname   = intent.getStringExtra("nickname")  ?: "유저"

        // 인사말
        val greeting = "${nickname} 님의 \"$videoTitle\"에 대한\n피드백 보고서가 도착했습니다📨"
        findViewById<TextView>(R.id.tvGreeting).text = greeting

    }
}