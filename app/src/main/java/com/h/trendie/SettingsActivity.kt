package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class SettingsActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<TextView>(R.id.tvTerms).setOnClickListener {
            Toast.makeText(this, "이용약관 (추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvNotice).setOnClickListener {
            Toast.makeText(this, "공지사항 (추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvCstService).setOnClickListener {
            Toast.makeText(this, "고객센터 (추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvAlarm).setOnClickListener {
            Toast.makeText(this, "알림 설정 (보류)", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvDisplaySet).setOnClickListener {
            Toast.makeText(this, "라이트/다크모드 (보류)", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val layoutDisplay = findViewById<LinearLayout>(R.id.layoutDisplayMode)
        val tvCurrentMode = findViewById<TextView>(R.id.tvCurrentMode)

        val prefs = getSharedPreferences("trendie_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        tvCurrentMode.text = if (isDarkMode) "다크모드" else "라이트모드"

        layoutDisplay.setOnClickListener {
            val newDarkMode = !prefs.getBoolean("dark_mode", false)
            prefs.edit().putBoolean("dark_mode", newDarkMode).apply()
            if (newDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            tvCurrentMode.text = if (newDarkMode) "다크모드" else "라이트모드"
        }

        // 로그아웃 버튼
        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val accessToken = userPrefs.getString("accessToken", "") ?: ""
            val refreshToken = userPrefs.getString("refreshToken", "") ?: ""

            val loginType = if (userPrefs.contains("kakaoAccessToken")) "kakao" else "google"
            val logoutUrl = if (loginType == "kakao") {
                "http://10.0.2.2:8000/api/v1/auth/kakao/logout"
            } else {
                "http://10.0.2.2:8000/api/v1/auth/google/logout"
            }

            // 로그아웃 요청
            val json = JSONObject().apply {
                put("accessToken", accessToken)
                put("refreshToken", refreshToken)
            }
            val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
            val request = Request.Builder()
                .url(logoutUrl)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    userPrefs.edit().clear().apply()
                    prefs.edit().clear().apply()
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "로그아웃 완료!", Toast.LENGTH_SHORT).show()
                        // 로그인 화면으로 이동
                        val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "로그아웃 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}