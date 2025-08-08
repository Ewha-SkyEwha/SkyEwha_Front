package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        val nickname = intent.getStringExtra("nickname")
        val nextTime = findViewById<TextView>(R.id.do_it_later)
        nextTime.paintFlags = nextTime.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.isEnabled = false  // 선택 전에는 비활성화

        val buttons = listOf(
            findViewById<ToggleButton>(R.id.mtn),
            findViewById<ToggleButton>(R.id.sea),
            findViewById<ToggleButton>(R.id.city)
        )

        // 다음 버튼 활성화 조건 체크
        buttons.forEach { button ->
            button.setOnClickListener {
                val selected = buttons.filter { it.isChecked }
                btnNext.isEnabled = selected.isNotEmpty()
                Log.d("선택된 선호", selected.toString())
            }
        }

        btnNext.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 지금은 넘어갈래요
        nextTime.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}