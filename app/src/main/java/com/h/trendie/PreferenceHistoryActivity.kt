package com.h.trendie

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.ui.theme.applyTopInsetPadding

class PreferenceHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_history)
        setupSimpleToolbar(R.string.title_settings)
        findViewById<android.view.View>(R.id.prefToolbar)?.applyTopInsetPadding()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val set = prefs.getStringSet("preference_choices", setOf()) ?: setOf()

        val items = set.toList().reversed().map { PreferenceItem(value = it) }
        recyclerView.adapter = PreferenceHistoryAdapter(items)
    }
}