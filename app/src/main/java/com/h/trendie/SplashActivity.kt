package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        // 최초 실행 시 install_date 저장
        if (prefs.getLong("install_date", 0L) == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong("install_date", now).apply()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            val nickname = prefs.getString("nickname", "")

            if (nickname.isNullOrEmpty()) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 800)
    }
}