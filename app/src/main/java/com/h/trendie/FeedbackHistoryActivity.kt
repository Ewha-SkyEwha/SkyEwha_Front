package com.h.trendie

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

        // 툴바
        setupSimpleToolbar(R.string.title_feedback_history)

        val db = AppDatabase.getInstance(this)

        val rv = findViewById<RecyclerView>(R.id.rvFeedbackHistory)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = FeedbackHistoryAdapter(mutableListOf()) { item ->
            // 보고서 화면으로 이동할 때 hideToolbar 플래그 추가
            startActivity(
                Intent(this, FeedbackReportActivity::class.java).apply {
                    putExtra("historyId", item.id)
                    putExtra("videoTitle", item.title)
                    putExtra("nickname", "유저닉네임") // DB에서 가져와서 넣기
                    putExtra("hideToolbar", true)      // 내역에서 툴바 숨김
                }
            )
        }
        rv.adapter = adapter

        lifecycleScope.launch {
            val rows = db.feedbackHistoryDao().getAll()
            adapter.updateData(rows.map { HistoryItem(it.id, it.title, it.date) })
        }

        val deco = DividerItemDecoration(this, LinearLayoutManager.VERTICAL).apply {
            ContextCompat.getDrawable(
                this@FeedbackHistoryActivity,
                R.drawable.divider_horizontal
            )?.let { setDrawable(it) }
        }
        rv.addItemDecoration(deco)
    }
}