package com.h.trendie

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.databinding.ActivityLoginBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // 안전한 저장소
    private val secureSp by lazy { securePrefs() }
    // 플로우/상태 저장소 (state, pendingProvider 등)
    private val flowSp by lazy { securePrefs() } // 필요하면 일반 SP로 분리 가능

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var lastProvider: String? = null // "google" | "kakao"
    @Volatile private var handlingCallback: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lastProvider = flowSp.getString(KEY_PENDING_PROVIDER, null)

        val googleBtn = findViewById<LinearLayout>(R.id.btnGoogle)
        val kakaoBtn  = findViewById<LinearLayout>(R.id.btnKakao)

        googleBtn.setOnClickListener {
            if (handlingCallback) return@setOnClickListener
            setButtonsEnabled(false)
            startLogin("google")
        }
        kakaoBtn.setOnClickListener {
            if (handlingCallback) return@setOnClickListener
            setButtonsEnabled(false)
            startLogin("kakao")
        }

        // 콜드/웜 스타트 진입 시 딥링크 처리
        handleAuthIntent(intent)
    }

    private fun startLogin(provider: String) {
        lastProvider = provider
        flowSp.edit().putString(KEY_PENDING_PROVIDER, provider).apply()

        // 1) CSRF 방지용 state 임시 생성/저장 (login_url에 state가 이미 있으면 나중에 '교체'함)
        val tentative = UUID.randomUUID().toString()
        flowSp.edit().putString(KEY_PENDING_STATE, tentative).apply()

        // 2) login_url 받아오고 → state 유무에 따라 최종 URL 결정
        getLoginUrlAndOpen(provider, tentative)
    }

    /** GET /api/v1/auth/{provider}/login_url  →  { "login_url": "..." } */
    private fun getLoginUrlAndOpen(provider: String, tentativeState: String) {
        Log.d(TAG, "[$provider] login_url 요청")

        val req = Request.Builder()
            .url("${ApiConfig.BASE_URL}/api/v1/auth/$provider/login_url")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "[$provider] login_url 실패: ${response.code}, ${safe(body)}")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "$provider 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                        setButtonsEnabled(true)
                    }
                    return
                }
                try {
                    val raw = JSONObject(body).optString("login_url").orEmpty()
                    require(raw.isNotBlank())

                    val rawUri = Uri.parse(raw)
                    val stateFromServer = rawUri.getQueryParameter("state")

                    // ✅ 서버가 이미 state를 넣어줬으면 그걸 '기대값'으로 사용 (덧붙이지 않음)
                    val finalState = stateFromServer ?: tentativeState
                    flowSp.edit().putString(KEY_PENDING_STATE, finalState).apply()

                    val finalUrl = if (stateFromServer.isNullOrBlank()) {
                        appendQuery(raw, "state" to finalState)
                    } else {
                        raw // 이미 있음
                    }

                    Log.d(TAG, "[$provider] 로그인 URL 최종: ${safe(finalUrl)} (state=$finalState)")
                    runOnUiThread { openLoginPage(finalUrl) }
                } catch (e: Exception) {
                    Log.e(TAG, "[$provider] login_url 파싱 실패: ${safe(body)}", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "로그인 URL 파싱 실패", Toast.LENGTH_SHORT).show()
                        setButtonsEnabled(true)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "[$provider] login_url onFailure: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "$provider 로그인 URL 요청 실패", Toast.LENGTH_SHORT).show()
                    setButtonsEnabled(true)
                }
            }
        })
    }

    private fun openLoginPage(url: String) {
        Log.d(TAG, "openLoginPage: ${safe(url)}")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        // 버튼은 브라우저 전환 후에도 비활성 상태 유지 → 실패 시에만 복구
    }

    /** 새 인텐트로 복귀(딥링크) */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    /** onCreate/onNewIntent 공통 처리: 딥링크 1회만 */
    private fun handleAuthIntent(i: Intent?) {
        val uri = i?.data ?: return

        // ✅ 액션이 꼭 VIEW가 아닐 수도 있으니 스킵하지 말고 스킴/호스트로 판별
        if (!isOurCallback(uri)) return
        if (handlingCallback) return

        Log.d(TAG, "딥링크 수신: ${safe(uri.toString())}")

        uri.getQueryParameter("error")?.let { err ->
            Log.e(TAG, "OAuth error: $err")
            handlingCallback = false
            clearPendingFlow()
            Toast.makeText(this, "로그인 실패: $err", Toast.LENGTH_SHORT).show()
            this.intent?.data = null
            setButtonsEnabled(true)
            return
        }

        val code  = uri.getQueryParameter("code") ?: run {
            Log.e(TAG, "code 파라미터 없음")
            this.intent?.data = null
            setButtonsEnabled(true)
            return
        }

        // ✅ state 검증: 다중 state 대비(getQueryParameters) + 우리가 저장한 기대값과 비교
        val expectedState = flowSp.getString(KEY_PENDING_STATE, null)
        if (!expectedState.isNullOrBlank()) {
            val states = uri.getQueryParameters("state") // 여러 개 있을 수 있음
            if (states.isNullOrEmpty() || !states.contains(expectedState)) {
                Log.e(TAG, "state mismatch: uri=$states, expected=$expectedState")
                Toast.makeText(this, "로그인 검증 실패. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                this.intent?.data = null
                setButtonsEnabled(true)
                return
            }
        }

        // provider 추정
        val provider = uri.getQueryParameter("provider")
            ?: lastProvider
            ?: flowSp.getString(KEY_PENDING_PROVIDER, null)
            ?: when {
                uri.toString().contains("google", true) -> "google"
                uri.toString().contains("kakao",  true) -> "kakao"
                else -> "google"
            }

        handlingCallback = true
        clearPendingFlow()
        Log.d(TAG, "콜백 수신: provider=$provider, code=${mask(code)}")

        // 최종 로그인 처리
        sendLoginCodeToServer(provider, code)

        // 같은 인텐트 재진입 시 중복 방지
        this.intent?.data = null
    }

    private fun isOurCallback(uri: Uri): Boolean =
        uri.scheme == "trendie" && uri.host == "oauth-callback"

    /** POST /api/v1/auth/{provider}/login { "code": "..." } */
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
                Log.d(TAG, "[$provider] /login 응답: ${safe(resStr)}")

                if (!response.isSuccessful) {
                    handlingCallback = false
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "로그인 실패(${response.code})", Toast.LENGTH_SHORT).show()
                        setButtonsEnabled(true)
                    }
                    return
                }
                handleLoginResponse(provider, resStr)
            }

            override fun onFailure(call: Call, e: IOException) {
                handlingCallback = false
                Log.e(TAG, "[$provider] /login onFailure: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "로그인 네트워크 오류", Toast.LENGTH_SHORT).show()
                    setButtonsEnabled(true)
                }
            }
        })
    }

    /** 공통 응답 처리 (신규/기존 분기) */
    private fun handleLoginResponse(provider: String, resStr: String) {
        try {
            val json = JSONObject(resStr)
            val isNewUser = when {
                json.has("isNewUser") -> json.optBoolean("isNewUser", false)
                json.isNull("user")   -> true
                else                  -> false
            }

            if (!isNewUser) {
                // 기존 유저 → 메인
                val access  = json.optString("accessToken", "")
                val refresh = json.optString("refreshToken", "")
                val user    = json.optJSONObject("user")
                val email   = user?.optString("email") ?: json.optString("email", "")

                saveTokens(access, refresh, provider, email)

                runOnUiThread {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                }
            } else {
                // 신규 유저 → 임시 데이터 안전 저장 후 화면 전환
                val tempToken = json.optString("tempToken", "")
                val email     = json.optString("email", "")
                val providerAccess = when (provider) {
                    "google" -> json.optString("google_access_token", "")
                    else     -> json.optString("kakao_access_token", "")
                }

                // ✅ 인텐트로 넘기지 말고 안전 저장(즉시 사용 후 삭제)
                secureSp.edit().apply {
                    putString(KEY_SIGNUP_PROVIDER, provider)
                    putString(KEY_SIGNUP_EMAIL, email)
                    putString(KEY_SIGNUP_TEMP_TOKEN, tempToken)
                    putString(KEY_SIGNUP_PROVIDER_ACCESS, providerAccess)
                    apply()
                }

                runOnUiThread {
                    startActivity(Intent(this@LoginActivity, NicknameActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                }
            }
        } catch (e: Exception) {
            handlingCallback = false
            Log.e(TAG, "응답 파싱 실패: ${safe(resStr)}", e)
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "서버 응답 파싱 실패", Toast.LENGTH_SHORT).show()
                setButtonsEnabled(true)
            }
        }
    }

    /** 최종 토큰 안전 저장 */
    private fun saveTokens(access: String, refresh: String, provider: String, email: String?) {
        if (access.isBlank()) return
        secureSp.edit().apply {
            putString(ApiConfig.KEY_ACCESS, access)
            putString(ApiConfig.KEY_REFRESH, refresh)
            putString(ApiConfig.KEY_PROVIDER, provider)
            if (!email.isNullOrBlank()) putString(ApiConfig.KEY_EMAIL, email)
            apply()
        }
        Log.d(TAG, "토큰 저장 완료, provider=$provider (access=${mask(access)}, refresh=${mask(refresh)})")
    }

    private fun clearPendingFlow() {
        flowSp.edit().remove(KEY_PENDING_PROVIDER).remove(KEY_PENDING_STATE).apply()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        findViewById<LinearLayout>(R.id.btnGoogle)?.isEnabled = enabled
        findViewById<LinearLayout>(R.id.btnKakao)?.isEnabled  = enabled
    }

    // ======== Util ========

    private fun securePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            this,
            ApiConfig.PREFS_USER,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun appendQuery(url: String, vararg pairs: Pair<String, String>): String {
        val hasQuery = url.contains("?")
        val sb = StringBuilder(url)
        for ((i, p) in pairs.withIndex()) {
            val sep = if (hasQuery || i > 0) "&" else "?"
            sb.append(sep)
                .append(p.first)
                .append("=")
                .append(URLEncoder.encode(p.second, "UTF-8"))
        }
        return sb.toString()
    }

    private fun mask(v: String?, keep: Int = 3): String {
        if (v.isNullOrBlank()) return ""
        val head = v.take(keep)
        val tail = v.takeLast(keep)
        return "$head...$tail"
    }

    private fun safe(s: String?): String = s?.let { if (it.length > 300) it.take(300) + "..." else it } ?: ""

    companion object {
        private const val TAG = "앱체크"

        private const val KEY_PENDING_PROVIDER = "pendingProvider"
        private const val KEY_PENDING_STATE    = "pendingState"

        private const val KEY_SIGNUP_PROVIDER        = "signup_provider"
        private const val KEY_SIGNUP_EMAIL           = "signup_email"
        private const val KEY_SIGNUP_TEMP_TOKEN      = "signup_temp_token"
        private const val KEY_SIGNUP_PROVIDER_ACCESS = "signup_provider_access"
    }
}