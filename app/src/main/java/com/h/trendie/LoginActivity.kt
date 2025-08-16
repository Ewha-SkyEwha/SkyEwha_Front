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

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val client = OkHttpClient()

    // 누른 로그인 버튼 기록 (콜백 구분용)
    private var lastProvider: String? = null // "kakao" | "google"

    // 브라우저 왕복 중 Activity 재시작 경우 기억 위한 임시 저장소
    private val flowSp by lazy { getSharedPreferences("auth_flow", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 재시작된 경우 pendingProvider 복구
        lastProvider = flowSp.getString("pendingProvider", null)

        val googleBtn = findViewById<LinearLayout>(R.id.btnGoogle)
        val kakaoBtn  = findViewById<LinearLayout>(R.id.btnKakao)

        googleBtn.setOnClickListener {
            lastProvider = "google"
            flowSp.edit().putString("pendingProvider", "google").apply() // 저장
            getLoginUrlAndOpen("google")
        }

        kakaoBtn.setOnClickListener {
            lastProvider = "kakao"
            flowSp.edit().putString("pendingProvider", "kakao").apply()
            getLoginUrlAndOpen("kakao")
        }

        // 새로 시작&딥링크로 들어온 경우 처리
        handleAuthIntent(intent)
    }

    /** 1) 서버에서 로그인 URL 받아서 브라우저 열기 */
    private fun getLoginUrlAndOpen(provider: String) {
        Log.d("앱체크", "$provider 로그인 URL 요청 시작!")
        val req = Request.Builder()
            .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/login_url")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("앱체크", "[$provider] login_url 실패: ${response.code}, $body")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "$provider 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val url = JSONObject(body).optString("login_url", "")
                    Log.d("앱체크", "[$provider] 로그인 URL: $url")
                    if (url.isBlank()) throw IllegalStateException("login_url 없음")
                    runOnUiThread { openLoginPage(url) }
                } catch (e: Exception) {
                    Log.e("앱체크", "[$provider] login_url 파싱 실패: $body", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "로그인 URL 파싱 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("앱체크", "[$provider] onFailure: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "$provider 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun openLoginPage(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** 2) 딥링크 콜백 (redirect_uri로 돌아왔을 때) - 새 인텐트로 들어오는 경우 */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)             // ✅ 인텐트 교체
        handleAuthIntent(intent)      // ✅ 공통 처리
    }

    /** 공통 콜백 처리 (onCreate/onNewIntent 양쪽에서 호출) */
    private fun handleAuthIntent(i: Intent?) {
        val uri = i?.data ?: return
        Log.d("앱체크", "딥링크 수신 uri=$uri")
        val code = uri.getQueryParameter("code") ?: run {
            Log.e("앱체크", "code 파라미터 없음!")
            return
        }

        // 버튼 누를 때 기록한 provider 우선, 없으면 저장소 복구, 그래도 없으면 추정
        val provider = lastProvider
            ?: flowSp.getString("pendingProvider", null)
            ?: if (uri.toString().contains("google", true)) "google" else "kakao"

        // 사용 끝났으면 정리
        flowSp.edit().remove("pendingProvider").apply()

        Log.d("앱체크", "콜백 수신: provider=$provider, code=$code")
        sendLoginCodeToServer(provider, code)
    }

    /** 3) code를 서버에 전달 → 기존/신규 분기 */
    private fun sendLoginCodeToServer(provider: String, code: String) {
        val json = JSONObject().apply { put("code", code) }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val req = Request.Builder()
            .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/login") // 👈 반드시 /login
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                Log.d("앱체크", "[$provider] /login 응답: $resStr")

                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "로그인 실패(${response.code})", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val jsonRes = JSONObject(resStr)
                    val isNewUser = jsonRes.optBoolean("isNewUser", jsonRes.isNull("user")) // 두 포맷 모두 대응
                    if (!isNewUser) {
                        // 기존 유저
                        val access = jsonRes.optString("accessToken", "")
                        val refresh = jsonRes.optString("refreshToken", "")
                        val user = jsonRes.optJSONObject("user")
                        val nickname = user?.optString("nickname") ?: jsonRes.optString("nickname", "")
                        val email = user?.optString("email") ?: jsonRes.optString("email", "")

                        saveAuth(access, refresh, provider, email)
                        runOnUiThread {
                            val intent = Intent(this@LoginActivity, PreferenceActivity::class.java)
                            intent.putExtra("nickname", nickname)
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // 신규 유저 → 닉네임 화면
                        val tempToken = jsonRes.optString("tempToken", "")
                        val email = jsonRes.optString("email", "")
                        val providerAccess = when (provider) {
                            "google" -> jsonRes.optString("google_access_token", "")
                            else     -> jsonRes.optString("kakao_access_token", "")
                        }

                        runOnUiThread {
                            val i = Intent(this@LoginActivity, NicknameActivity::class.java)
                            i.putExtra("provider", provider)
                            i.putExtra("email", email)
                            i.putExtra("tempToken", tempToken)
                            i.putExtra("providerAccessToken", providerAccess)
                            startActivity(i)
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("앱체크", "[$provider] /login 파싱 실패: $resStr", e)
                    runOnUiThread { Toast.makeText(this@LoginActivity, "서버 응답 파싱 실패", Toast.LENGTH_SHORT).show() }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("앱체크", "[$provider] /login onFailure: ${e.message}", e)
                runOnUiThread { Toast.makeText(this@LoginActivity, "로그인 네트워크 오류", Toast.LENGTH_SHORT).show() }
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