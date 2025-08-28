package com.h.trendie

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

class PreferenceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        // 화면 확장 (시스템 바 뒤로)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 최상위 레이아웃 안전영역 패딩
        val content = findViewById<ViewGroup>(android.R.id.content)
        val root: View = content.getChildAt(0) ?: content
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // "지금은 넘어갈래요"/다음 버튼 제스처바에 안 가려지게 하단 패딩
            val extra = (16 * resources.displayMetrics.density).toInt()

            v.updatePadding(
                top = max(v.paddingTop, sysBars.top),
                // 키보드가 올라올 때
                bottom = max(max(v.paddingBottom, sysBars.bottom + extra), ime.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)

        val nickname = intent.getStringExtra("nickname")

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

        fun refreshNextEnabled() {
            btnNext.isEnabled = toggles.any { it.isChecked }
        }

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
            Log.d("Pref", "메인으로 이동 시도")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}