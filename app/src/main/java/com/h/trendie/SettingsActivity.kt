package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.h.trendie.ui.theme.applyTopInsetPadding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    private val client by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupSimpleToolbar(R.string.title_settings)

        // toolbar (include 된 view_simple_toolbar)
        findViewById<View>(R.id.settingsToolbar)?.applyTopInsetPadding()
        findViewById<TextView>(R.id.tvTitle)?.text = getString(R.string.title_settings)
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 단순 항목 클릭 토스트(추후 화면 연결)
        findViewById<TextView>(R.id.tvTerms).setOnClickListener {
            Toast.makeText(this, "이용약관 (추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvNotice).setOnClickListener {
            Toast.makeText(this, "공지사항 (추후 연결)", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvCstService).setOnClickListener {
            Toast.makeText(this, "고객센터 (추후 연결)", Toast.LENGTH_SHORT).show()
        }

        // 라이트/다크모드
        val layoutDisplay = findViewById<LinearLayout>(R.id.layoutDisplayMode)
        val tvCurrentMode = findViewById<TextView>(R.id.tvCurrentMode)
        val appPrefs = getSharedPreferences(ApiConfig.PREFS_APP, MODE_PRIVATE)
        tvCurrentMode.text = if (appPrefs.getBoolean("dark_mode", false)) "다크모드" else "라이트모드"

        layoutDisplay.setOnClickListener {
            val newDark = !appPrefs.getBoolean("dark_mode", false)
            appPrefs.edit().putBoolean("dark_mode", newDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (newDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            tvCurrentMode.text = if (newDark) "다크모드" else "라이트모드"
        }

        // 로그아웃
        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            val userSp = getSharedPreferences(ApiConfig.PREFS_USER, MODE_PRIVATE)
            val provider = userSp.getString(ApiConfig.KEY_PROVIDER, null) // "kakao" | "google"

            fun clearAndGoLogin() {
                userSp.edit().clear().apply()
                getSharedPreferences(ApiConfig.PREFS_APP, MODE_PRIVATE).edit().clear().apply()
                val i = Intent(this, LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
                finish()
            }

            if (provider.isNullOrBlank()) {
                clearAndGoLogin()
                return@setOnClickListener
            }

            val req = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/logout")
                .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : okhttp3.Callback {
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    runOnUiThread { clearAndGoLogin() }
                }
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    runOnUiThread { clearAndGoLogin() }
                }
            })
        }
    }
}