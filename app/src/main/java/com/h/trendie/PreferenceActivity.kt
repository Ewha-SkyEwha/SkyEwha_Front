package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.graphics.Paint
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class PreferenceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        val nickname = intent.getStringExtra("nickname") // 필요시 활용

        val nextTime = findViewById<TextView>(R.id.do_it_later).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }
        val btnNext = findViewById<Button>(R.id.btnNext).apply {
            isEnabled = false // 선택 전엔 비활성화
        }

        val mtn  = findViewById<ToggleButton>(R.id.mtn)
        val sea  = findViewById<ToggleButton>(R.id.sea)
        val city = findViewById<ToggleButton>(R.id.city)
        val toggles = listOf(mtn, sea, city)

        // 다음 버튼 활성화 상태 갱신
        fun refreshNextEnabled() {
            btnNext.isEnabled = toggles.any { it.isChecked }
        }

        // 토글 변경 시마다 상태 갱신
        toggles.forEach { toggle ->
            toggle.setOnCheckedChangeListener { _, _ -> refreshNextEnabled() }
        }

        btnNext.setOnClickListener {
            val selected = toggles
                .filter { it.isChecked }
                .map { it.text.toString() }
                .toSet()

            getSharedPreferences(ApiConfig.PREFS_USER, MODE_PRIVATE)
                .edit()
                .putStringSet(ApiConfig.KEY_PREF_CHOICES, selected)
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // “지금은 넘어갈래요”
        nextTime.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}