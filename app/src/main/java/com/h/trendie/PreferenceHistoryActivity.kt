package com.h.trendie

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PreferenceHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_history)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val set = prefs.getStringSet("preference_choices", setOf()) ?: setOf()

        val items = set.toList().reversed().map {
            PreferenceItem("나의 여행 취향", it)
        }

        recyclerView.adapter = PreferenceHistoryAdapter(items)

        // 뒤로가기
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }
}