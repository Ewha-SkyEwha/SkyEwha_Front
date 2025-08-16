package com.h.trendie

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.data.AppDatabase
import kotlinx.coroutines.launch
import FeedbackHistoryAdapter

class FeedbackHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_history)

        // 공통 툴바
        setupSimpleToolbar(R.string.title_feedback_history)

        // RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvFeedbackHistory)
        rv.layoutManager = LinearLayoutManager(this)

        val adapter = FeedbackHistoryAdapter(mutableListOf())
        rv.adapter = adapter

        // Divider
        val deco = DividerItemDecoration(this, LinearLayoutManager.VERTICAL).apply {
            ContextCompat.getDrawable(this@FeedbackHistoryActivity, R.drawable.divider_horizontal)
                ?.let { setDrawable(it) }
        }
        rv.addItemDecoration(deco)

        // Room에서 불러와 어댑터 갱신
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val list = db.feedbackHistoryDao().getAll()
            // HistoryItem(title, date)로 매핑
            adapter.updateData(list.map { HistoryItem(it.title, it.date) })
        }
    }
}