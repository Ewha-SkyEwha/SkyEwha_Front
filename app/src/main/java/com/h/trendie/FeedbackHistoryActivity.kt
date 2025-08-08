package com.h.trendie

import FeedbackHistoryAdapter
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.data.AppDatabase
import kotlinx.coroutines.launch

class FeedbackHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_history)

        val rv = findViewById<RecyclerView>(R.id.rvFeedbackHistory)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = FeedbackHistoryAdapter(mutableListOf())
        rv.adapter = adapter

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()  // 종료하고 이전 화면으로
        }

        // Room에서 불러오기
        val db = AppDatabase.Companion.getInstance(this)
        lifecycleScope.launch {
            val list = db.feedbackHistoryDao().getAll()
            adapter.updateData(list.map { HistoryItem(it.title, it.date) })
        }
    }
}