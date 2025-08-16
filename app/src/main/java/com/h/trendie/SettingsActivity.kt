package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.h.trendie.ui.theme.applyTopInsetPadding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class SettingsActivity : AppCompatActivity() {
    private val client = OkHttpClient()


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("SETTINGS", "onCreate")
        Toast.makeText(this, "설정 화면 진입", Toast.LENGTH_SHORT).show()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.settingsToolbar)?.applyTopInsetPadding()

        findViewById<TextView>(R.id.tvTitle)?.text = getString(R.string.title_settings)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

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
            Toast.makeText(this, "라이트/다크모드", Toast.LENGTH_SHORT).show()
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
            val userSp = getSharedPreferences(ApiConfig.PREFS_USER, MODE_PRIVATE)
            val provider = userSp.getString(ApiConfig.KEY_PROVIDER, null) // "kakao" | "google" (Login/NicknameActivity에서 저장함)

            fun clearAndGoLogin() {
                // 로컬 세션 정리
                userSp.edit().clear().apply()
                getSharedPreferences(ApiConfig.PREFS_APP, MODE_PRIVATE).edit().clear().apply()

                // 로그인 화면으로
                val i = Intent(this, LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
                finish()
            }

            // provider 없으면 서버 호출 없이 바로 정리
            if (provider.isNullOrBlank()) {
                clearAndGoLogin()
                return@setOnClickListener
            }

            // 서버 로그아웃 호출
            val req = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/logout")
                .post(RequestBody.create("application/json".toMediaTypeOrNull(), "{}"))
                .build()

            OkHttpClient().newCall(req).enqueue(object : okhttp3.Callback {
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    // 로컬 세션 정리
                    runOnUiThread { clearAndGoLogin() }
                }
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    runOnUiThread { clearAndGoLogin() }
                }
            })
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("SETTINGS", "onDestroy")
    }
}

/* 응답 URL 열어야 되면
val res = JSONObject(resStr)
val kakaoUrl = res.optString("kakao_logout_url", "")
if (kakaoUrl.isNotBlank()) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(kakaoUrl)))
}
clearAndGoLogin() */