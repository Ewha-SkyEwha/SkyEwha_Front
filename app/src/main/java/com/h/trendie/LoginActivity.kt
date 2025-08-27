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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val client by lazy { OkHttpClient() }

    // 마지막으로 누른 프로바이더 (콜백 구분용)
    private var lastProvider: String? = null // "kakao" | "google"

    // 브라우저 왕복 대비 (액티비티 재생성 시 복구용)
    private val flowSp by lazy { getSharedPreferences("auth_flow", MODE_PRIVATE) }

    // 딥링크 콜백 중복 방지
    @Volatile private var handlingCallback: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 재시작 복구
        lastProvider = flowSp.getString("pendingProvider", null)

        val googleBtn = findViewById<LinearLayout>(R.id.btnGoogle)
        val kakaoBtn  = findViewById<LinearLayout>(R.id.btnKakao)

        googleBtn.setOnClickListener {
            if (handlingCallback) return@setOnClickListener
            lastProvider = "google"
            flowSp.edit().putString("pendingProvider", "google").apply()
            getLoginUrlAndOpen("google")
        }

        kakaoBtn.setOnClickListener {
            if (handlingCallback) return@setOnClickListener
            lastProvider = "kakao"
            flowSp.edit().putString("pendingProvider", "kakao").apply()
            getLoginUrlAndOpen("kakao")
        }

        // 앱 시작/재진입 인텐트 처리
        handleAuthIntent(intent)
    }

    /** 1) 서버에서 로그인 URL 획득 → 브라우저 열기 */
    private fun getLoginUrlAndOpen(provider: String) {
        Log.d("앱체크", "[$provider] login_url 요청")
        val req = Request.Builder()
            .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/login_url")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e("앱체크", "[$provider] login_url 실패: ${response.code}, $body")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "$provider 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val url = JSONObject(body).optString("login_url").orEmpty()
                    require(url.isNotBlank())
                    Log.d("앱체크", "[$provider] 로그인 URL: $url")
                    runOnUiThread { openLoginPage(url) }
                } catch (e: Exception) {
                    Log.e("앱체크", "[$provider] login_url 파싱 실패: $body", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "로그인 URL 파싱 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("앱체크", "[$provider] login_url onFailure: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "$provider 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun openLoginPage(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** 2) 딥링크 콜백: 새 인텐트 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    /** onCreate/onNewIntent 공통 처리 */
    private fun handleAuthIntent(i: Intent?) {
        val uri = i?.data ?: return
        if (i.action != Intent.ACTION_VIEW) return
        if (handlingCallback) return

        Log.d("앱체크", "딥링크 수신: $uri")

        // OAuth 에러
        uri.getQueryParameter("error")?.let { err ->
            Log.e("앱체크", "OAuth error: $err")
            handlingCallback = false
            flowSp.edit().remove("pendingProvider").apply()
            Toast.makeText(this, "로그인 실패: $err", Toast.LENGTH_SHORT).show()
            return
        }

        val code = uri.getQueryParameter("code") ?: run {
            Log.e("앱체크", "code 파라미터 없음")
            return
        }

        val provider = lastProvider
            ?: flowSp.getString("pendingProvider", null)
            ?: when {
                uri.toString().contains("google", true) -> "google"
                uri.toString().contains("kakao",  true) -> "kakao"
                else -> "google"
            }

        handlingCallback = true
        flowSp.edit().remove("pendingProvider").apply()
        Log.d("앱체크", "콜백 수신: provider=$provider, code=$code")

        sendLoginCodeToServer(provider, code)
    }

    /** 3) code 서버 전송 → 기존/신규 분기 */
    private fun sendLoginCodeToServer(provider: String, code: String) {
        val json = JSONObject().apply { put("code", code) }.toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/login")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string().orEmpty()
                Log.d("앱체크", "[$provider] /login 응답: $resStr")

                if (!response.isSuccessful) {
                    handlingCallback = false
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "로그인 실패(${response.code})", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val jsonRes = JSONObject(resStr)

                    // 백엔드 응답 양식 여러 케이스 방어
                    val isNewUser = when {
                        jsonRes.has("isNewUser") -> jsonRes.optBoolean("isNewUser", false)
                        jsonRes.isNull("user")   -> true
                        else                     -> false
                    }

                    if (!isNewUser) {
                        // === 기존 유저 → 바로 메인 ===
                        val access  = jsonRes.optString("accessToken", "")
                        val refresh = jsonRes.optString("refreshToken", "")
                        val user    = jsonRes.optJSONObject("user")
                        val email   = user?.optString("email") ?: jsonRes.optString("email", "")

                        saveAuth(access, refresh, provider, email)

                        runOnUiThread {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        // === 신규 유저 → 닉네임 화면 ===
                        val tempToken = jsonRes.optString("tempToken", "")
                        val email     = jsonRes.optString("email", "")
                        val providerAccess = when (provider) {
                            "google" -> jsonRes.optString("google_access_token", "")
                            else     -> jsonRes.optString("kakao_access_token", "")
                        }

                        runOnUiThread {
                            val i = Intent(this@LoginActivity, NicknameActivity::class.java).apply {
                                putExtra("provider", provider)
                                putExtra("email", email)
                                putExtra("tempToken", tempToken)
                                putExtra("providerAccessToken", providerAccess)
                            }
                            startActivity(i)
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    handlingCallback = false
                    Log.e("앱체크", "[$provider] /login 파싱 실패: $resStr", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "서버 응답 파싱 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                handlingCallback = false
                Log.e("앱체크", "[$provider] /login onFailure: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "로그인 네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /** 토큰/프로바이더 저장 */
    private fun saveAuth(access: String, refresh: String, provider: String, email: String?) {
        if (access.isBlank()) return
        val sp = getSharedPreferences(ApiConfig.PREFS_USER, MODE_PRIVATE)
        sp.edit().apply {
            putString(ApiConfig.KEY_ACCESS, access)
            putString(ApiConfig.KEY_REFRESH, refresh)
            putString(ApiConfig.KEY_PROVIDER, provider)
            if (!email.isNullOrBlank()) putString(ApiConfig.KEY_EMAIL, email)
            apply()
        }
        Log.d("앱체크", "토큰 저장 완료, provider=$provider")
    }
}