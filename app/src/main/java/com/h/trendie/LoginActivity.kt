package com.h.trendie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.h.trendie.databinding.ActivityLoginBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val googleBtn = findViewById<LinearLayout>(R.id.btnGoogle)
        val kakaoBtn = findViewById<LinearLayout>(R.id.btnKakao)

        googleBtn.setOnClickListener {
            getGoogleLoginUrlAndOpen()
        }
        kakaoBtn.setOnClickListener {
            getKakaoLoginUrlAndOpen()
        }
    }

    // 카카오
    private fun getKakaoLoginUrlAndOpen() {
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/auth/kakao/login_url")
            .get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "카카오 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val url = JSONObject(resStr).getString("login_url")
                runOnUiThread { openLoginPage(url) }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "카카오 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun sendKakaoLoginCodeToServer(code: String) {
        val json = JSONObject().apply { put("code", code) }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/auth/kakao/login")
            .post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "카카오 로그인 실패", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val jsonResponse = JSONObject(resStr)
                val user = jsonResponse.optJSONObject("user")
                if (user != null) {
                    // 기존 유저: 바로 이동
                    val nickname = user.optString("nickname", "")
                    val email = user.optString("email", "")
                    val accessToken = jsonResponse.optString("accessToken", "")
                    val refreshToken = jsonResponse.optString("refreshToken", "")
                    saveAuthToken(accessToken, refreshToken)
                    val intent = Intent(this@LoginActivity, PreferenceActivity::class.java)
                    intent.putExtra("nickname", nickname)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    // 신규 유저: tempToken, kakao_access_token 전달
                    val intent = Intent(this@LoginActivity, NicknameActivity::class.java)
                    intent.putExtra("email", jsonResponse.optString("email", ""))
                    intent.putExtra("tempToken", jsonResponse.optString("tempToken", ""))
                    intent.putExtra("kakaoAccessToken", jsonResponse.optString("kakao_access_token", ""))
                    startActivity(intent)
                    finish()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "카카오 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // 구글
    private fun getGoogleLoginUrlAndOpen() {
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/auth/google/login_url")
            .get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "구글 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val url = JSONObject(resStr).getString("login_url")
                runOnUiThread { openLoginPage(url) }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "구글 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun sendGoogleLoginCodeToServer(code: String) {
        val json = JSONObject().apply { put("code", code) }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/auth/google/login_url")
            .post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val jsonResponse = JSONObject(resStr)
                val user = jsonResponse.optJSONObject("user")
                if (user != null) {
                    // 기존 유저
                    val nickname = user.optString("nickname", "")
                    val email = user.optString("email", "")
                    val accessToken = jsonResponse.optString("accessToken", "")
                    val refreshToken = jsonResponse.optString("refreshToken", "")
                    saveAuthToken(accessToken, refreshToken)
                    val intent = Intent(this@LoginActivity, PreferenceActivity::class.java)
                    intent.putExtra("nickname", nickname)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    // 신규 유저
                    val intent = Intent(this@LoginActivity, NicknameActivity::class.java)
                    intent.putExtra("email", jsonResponse.optString("email", ""))
                    intent.putExtra("tempToken", jsonResponse.optString("tempToken", ""))
                    intent.putExtra("googleAccessToken", jsonResponse.optString("google_access_token", ""))
                    startActivity(intent)
                    finish()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun openLoginPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    // 콜백
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri = intent?.data
        val code = uri?.getQueryParameter("code")
        if (code != null) {
            if (uri.toString().contains("kakao")) {
                sendKakaoLoginCodeToServer(code)
            } else {
                sendGoogleLoginCodeToServer(code)
            }
        }
    }

    // 토큰 저장
    private fun saveAuthToken(accessToken: String, refreshToken: String) {
        if (accessToken.isEmpty()) return
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().putString("accessToken", accessToken)
            .putString("refreshToken", refreshToken)
            .apply()
    }
}