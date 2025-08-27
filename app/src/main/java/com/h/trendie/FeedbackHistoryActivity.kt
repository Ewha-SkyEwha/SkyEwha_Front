package com.h.trendie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.data.AppDatabase
import kotlinx.coroutines.launch

class FeedbackHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: FeedbackHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_history)

        // 상단 툴바 타이틀
        setupSimpleToolbar(R.string.title_feedback_history)

        val db = AppDatabase.getInstance(this)

        val rv = findViewById<RecyclerView>(R.id.rvFeedbackHistory).apply {
            layoutManager = LinearLayoutManager(this@FeedbackHistoryActivity)
        }

        adapter = FeedbackHistoryAdapter(mutableListOf()) { item ->
            // ▶ 보고서 화면으로 이동할 때 저장된 닉네임을 함께 전달
            val nickname = readNickname()
            startActivity(
                Intent(this, FeedbackReportActivity::class.java).apply {
                    putExtra("historyId", item.id)
                    putExtra("videoTitle", item.title)
                    putExtra("nickname", nickname)  // ✅ 실제 닉네임
                    putExtra("hideToolbar", true)   // 내역에서 열면 툴바 숨김
                }
            )
        }
        rv.adapter = adapter

        // 구분선
        val deco = DividerItemDecoration(this, LinearLayoutManager.VERTICAL).apply {
            ContextCompat.getDrawable(
                this@FeedbackHistoryActivity,
                R.drawable.divider_horizontal
            )?.let { setDrawable(it) }
        }
        rv.addItemDecoration(deco)

        // 목록 로드
        lifecycleScope.launch {
            val rows = db.feedbackHistoryDao().getAll()
            adapter.updateData(rows.map { HistoryItem(it.id, it.title, it.date) })
        }
    }

    /** user_prefs에서 닉네임 읽기 (없으면 "유저") */
    private fun readNickname(): String =
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("nickname", "유저") ?: "유저"
}