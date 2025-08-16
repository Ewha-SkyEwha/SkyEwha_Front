package com.h.trendie

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.ui.theme.applyTopInsetPadding

class PreferenceHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_history)
        setupSimpleToolbar(R.string.title_settings)

        findViewById<View>(R.id.prefToolbar)?.applyTopInsetPadding()
        findViewById<TextView>(R.id.tvTitle)?.text =
            getString(R.string.title_preference_history)
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val set = prefs.getStringSet("preference_choices", setOf()) ?: setOf()

        val items = set.toList().reversed().map {
            PreferenceItem("나의 여행 취향", it)
        }

        recyclerView.adapter = PreferenceHistoryAdapter(items)

    }
}