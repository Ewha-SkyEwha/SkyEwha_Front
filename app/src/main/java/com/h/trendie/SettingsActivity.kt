package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.h.trendie.network.ApiClient
import com.h.trendie.setupSimpleToolbarText
import com.h.trendie.util.NicknameStore
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    private val client by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupSimpleToolbarText("설정")

        val comingSoonClick = View.OnClickListener {
            Toast.makeText(this, "준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        findViewById<View?>(R.id.rowTerms)?.setOnClickListener(comingSoonClick)
        findViewById<View?>(R.id.rowNotice)?.setOnClickListener(comingSoonClick)
        findViewById<View?>(R.id.rowCs)?.setOnClickListener(comingSoonClick)
        findViewById<View?>(R.id.rowAlarm)?.setOnClickListener(comingSoonClick)

        val layoutDisplay = findViewById<LinearLayout>(R.id.layoutDisplayMode)
        val tvCurrentMode = findViewById<TextView>(R.id.tvCurrentMode)
        val appPrefs = getSharedPreferences(ApiConfig.PREFS_APP, MODE_PRIVATE)
        tvCurrentMode.text =
            if (appPrefs.getBoolean("dark_mode", false)) "다크모드" else "라이트모드"

        layoutDisplay.setOnClickListener {
            val newDark = !appPrefs.getBoolean("dark_mode", false)
            appPrefs.edit().putBoolean("dark_mode", newDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (newDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            tvCurrentMode.text = if (newDark) "다크모드" else "라이트모드"
        }

        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            val userSp = getSharedPreferences(ApiConfig.PREFS_USER, MODE_PRIVATE)
            val provider = userSp.getString(ApiConfig.KEY_PROVIDER, null)

            fun clearAndGoLogin() {
                ApiClient.setJwt(null)

                userSp.edit().clear().apply()
                getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences(ApiConfig.PREFS_APP, MODE_PRIVATE).edit().clear().apply()

                lifecycleScope.launch {
                    UserPrefs.setNickname(this@SettingsActivity, "")
                }
                NicknameStore.set(this@SettingsActivity, "")

                val i = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(i)
                finish()
            }

            if (provider.isNullOrBlank()) {
                clearAndGoLogin(); return@setOnClickListener
            }

            val req = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/logout")
                .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : okhttp3.Callback {
                override fun onResponse(
                    call: okhttp3.Call,
                    response: okhttp3.Response
                ) {
                    runOnUiThread { clearAndGoLogin() }
                }

                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    runOnUiThread { clearAndGoLogin() }
                }
            })
        }

        val root = findViewById<View>(R.id.rootSettings)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.systemGestures()
            )
            val extra = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(bottom = maxOf(v.paddingBottom, sys.bottom + extra))
            insets
        }
    }
}
