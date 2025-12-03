package com.h.trendie

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.auth.TokenProvider
import com.h.trendie.data.auth.OAuthLoginRes
import com.h.trendie.databinding.ActivityLoginBinding
import com.h.trendie.network.ApiClient
import com.h.trendie.network.ApiService
import com.h.trendie.util.NicknameStore
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import java.net.URLEncoder
import java.util.UUID

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val api: ApiService by lazy { ApiClient.apiService }

    private val secureSp by lazy { securePrefs(ApiConfig.PREFS_USER) }
    // 임시 플로우 상태 저장
    private val flowSp by lazy { securePrefs(ApiConfig.PREFS_FLOW) }

    private var lastProvider: String? = null
    @Volatile private var handlingCallback: Boolean = false
    private var lastCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lastProvider = flowSp.getString(KEY_PENDING_PROVIDER, null)

        binding.btnGoogle.setOnClickListener {
            if (handlingCallback) return@setOnClickListener
            setButtonsEnabled(false)
            startLoginBrowser("google")
        }

        binding.btnKakao.setOnClickListener {
            if (handlingCallback) return@setOnClickListener
            setButtonsEnabled(false)
            startKakaoSdkLogin()
        }

        handleAuthIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    override fun onDestroy() {
        handlingCallback = false
        super.onDestroy()
    }

    // Kakao SDK ---------------

    private fun startKakaoSdkLogin() {
        lastProvider = "kakao"
        flowSp.edit().putString(KEY_PENDING_PROVIDER, "kakao").apply()

        handlingCallback = true
        UserApiClient.instance.loginWithKakaoTalk(this) { token: OAuthToken?, error ->
            if (error != null) {
                loginWithKakaoAccountFallback()
            } else if (token != null) {
                sendKakaoAccessTokenToServer(token.accessToken)
            } else {
                onKakaoFail("알 수 없는 오류")
            }
        }
    }

    private fun loginWithKakaoAccountFallback() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token: OAuthToken?, error ->
            if (error != null) {
                onKakaoFail(error.message ?: "카카오 로그인 실패")
            } else if (token != null) {
                sendKakaoAccessTokenToServer(token.accessToken)
            } else {
                onKakaoFail("알 수 없는 오류")
            }
        }
    }

    private fun onKakaoFail(msg: String) {
        handlingCallback = false
        setButtonsEnabled(true)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun sendKakaoAccessTokenToServer(accessToken: String) {
        lifecycleScope.launch {
            val res = try {
                withTimeout(12_000) {
                    withContext(Dispatchers.IO) {
                        api.kakaoLoginPost(mapOf("access_token" to accessToken))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "kakao login(token) post failed/timeout", e)
                null
            }

            if (res != null && res.isSuccessful && res.body() != null) {
                try {
                    handleLoginResponse("kakao", res.body()!!)
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "handleLoginResponse failed (kakao)", e)
                }
            } else {
                if (res?.code() == 422) {
                    Log.w(
                        TAG,
                        "kakao token login 422 → switch to browser(code). body=${res.errorText()}"
                    )
                    handlingCallback = false
                    setButtonsEnabled(true)
                    startLoginBrowser("kakao")
                    return@launch
                }
                Log.e(
                    TAG,
                    "kakao token login failed code=${res?.code()} body=${res?.errorText()}"
                )
            }

            onKakaoFail("카카오 로그인 실패${res?.code()?.let { " ($it)" } ?: ""}")
        }
    }

    // Google/Kakao 브라우저(code) -------------

    private fun startLoginBrowser(provider: String) {
        lastProvider = provider
        flowSp.edit().putString(KEY_PENDING_PROVIDER, provider).apply()

        val tentative = UUID.randomUUID().toString()
        flowSp.edit().putString(KEY_PENDING_STATE, tentative).apply()
        getLoginUrlAndOpen(provider, tentative)
    }

    private fun getLoginUrlAndOpen(provider: String, tentativeState: String) {
        Log.d(TAG, "[$provider] login_url 요청")
        lifecycleScope.launch {
            val raw: String? = withContext(Dispatchers.IO) {
                try {
                    when (provider) {
                        "kakao" -> api.getKakaoLoginUrl()
                            .let { if (it.isSuccessful) it.body()?.loginUrl else null }

                        "google" -> api.getGoogleLoginUrl()
                            .let { if (it.isSuccessful) it.body()?.loginUrl else null }

                        else -> api.getKakaoLoginUrl()
                            .let { if (it.isSuccessful) it.body()?.loginUrl else null }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "login_url call failed", e)
                    null
                }
            }

            if (raw.isNullOrBlank()) {
                Toast.makeText(
                    this@LoginActivity,
                    "$provider 로그인 URL 요청 실패",
                    Toast.LENGTH_SHORT
                ).show()
                setButtonsEnabled(true)
                return@launch
            }

            val rawUri = Uri.parse(raw)
            val stateFromServer = rawUri.getQueryParameter("state")
            val finalState = stateFromServer ?: tentativeState
            flowSp.edit().putString(KEY_PENDING_STATE, finalState).apply()

            val finalUrl = if (stateFromServer.isNullOrBlank()) {
                appendQuery(raw, "state" to finalState)
            } else raw

            handlingCallback = false
            setButtonsEnabled(true)
            openLoginPage(finalUrl)
        }
    }

    private fun openLoginPage(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    // 딥링크 복귀

    private fun handleAuthIntent(i: Intent?) {
        val uri = i?.data ?: return
        Log.d(TAG, "deeplink uri=$uri handling=$handlingCallback")

        if (!isOurCallback(uri)) return

        val code = uri.getQueryParameter("code") ?: return

        val stateInUri = uri.getQueryParameter("state")
        val stateSaved = flowSp.getString(KEY_PENDING_STATE, null)
        if (!stateInUri.isNullOrEmpty() && stateSaved != null && stateInUri != stateSaved) {
            Log.e(TAG, "state mismatch: saved=$stateSaved, uri=$stateInUri")
            Toast.makeText(
                this,
                "로그인 상태가 올바르지 않습니다. 다시 시도해 주세요.",
                Toast.LENGTH_SHORT
            ).show()
            handlingCallback = false
            setButtonsEnabled(true)
            clearPendingFlow()
            return
        }

        val provider = uri.getQueryParameter("provider")
            ?: lastProvider
            ?: flowSp.getString(KEY_PENDING_PROVIDER, null)
            ?: if (uri.toString().contains("google", true)) "google" else "kakao"

        if (handlingCallback) {
            Log.w(TAG, "already handling → ignore (provider=$provider)")
            return
        }
        if (lastCode == code) {
            Log.w(TAG, "same code received again → ignore (provider=$provider)")
            return
        }

        handlingCallback = true
        lastCode = code
        clearPendingFlow()

        this.intent?.data = null
        i.data = null

        setButtonsEnabled(false)
        sendLoginCodeToServer(provider, code)
    }

    private fun isOurCallback(uri: Uri): Boolean {
        if (uri.scheme != "trendie") return false
        if (uri.host == "oauth" && uri.path == "/callback") return true
        if (uri.host == "kakao-callback") return true
        return false
    }

    private fun sendLoginCodeToServer(provider: String, code: String) {
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    when (provider.lowercase()) {
                        "kakao" -> api.kakaoLoginPost(mapOf("code" to code))
                        "google" -> api.googleLoginPost(mapOf("code" to code))
                        else -> api.kakaoLoginPost(mapOf("code" to code))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "login(code) post failed", e)
                    null
                }
            }

            if (res == null || !res.isSuccessful || res.body() == null) {
                Log.e(
                    TAG,
                    "login(code) failed provider=$provider code=${res?.code()} body=${res?.errorText()}"
                )
                handlingCallback = false
                Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                setButtonsEnabled(true)
                return@launch
            }

            val body = res.body()!!
            try {
                handleLoginResponse(provider, body)
            } catch (e: Exception) {
                Log.e(TAG, "handleLoginResponse failed for provider=$provider", e)
                handlingCallback = false
                Toast.makeText(
                    this@LoginActivity,
                    "서버 응답 처리 실패",
                    Toast.LENGTH_SHORT
                ).show()
                setButtonsEnabled(true)
            }
        }
    }

    private fun handleLoginResponse(provider: String, body: OAuthLoginRes) {
        if (!body.isNewUser) {
            val access = body.accessToken.orEmpty()
            val refresh = body.refreshToken.orEmpty()
            val email = body.user?.email
            val nick = body.user?.nickname
            val userId: Long? = body.user?.userId
            finishLoginSuccess(access, refresh, provider, email, nick, userId)
            return
        }

        // 신규 유저: 닉네임 화면
        val tempToken = body.tempToken.orEmpty()
        val email = body.email.orEmpty()
        val providerAccess = when (provider.lowercase()) {
            "google" -> body.googleAccessToken.orEmpty()
            else -> body.kakaoAccessToken.orEmpty()
        }
        goNicknameFlow(provider, email, tempToken, providerAccess)
    }

    // 기존 유저: 토큰/닉네임 저장 후 메인으로
    private fun finishLoginSuccess(
        access: String,
        refresh: String,
        provider: String,
        email: String?,
        nick: String?,
        userId: Long?
    ) {
        val pure = access.removePrefix("Bearer ").removePrefix("bearer ").trim()
        if (pure.isBlank()) {
            handlingCallback = false
            Toast.makeText(this, "액세스 토큰 없음", Toast.LENGTH_SHORT).show()
            setButtonsEnabled(true)
            return
        }

        ApiClient.setJwt(pure)
        mirrorRefreshForAuthenticator(refresh)

        // 1) 보안 prefs 저장
        saveTokens(pure, refresh, provider, email, nick, userId)

        // 2) 닉네임 상태 덮어쓰기 (DataStore + 로컬 캐시)
        val nicknameToUse = nick.orEmpty()
        lifecycleScope.launch {
            UserPrefs.setNickname(this@LoginActivity, nicknameToUse)
        }
        NicknameStore.set(this, nicknameToUse)

        Log.d(TAG, "Access token saved. preview=${TokenProvider.preview(this)}")

        handlingCallback = false
        setButtonsEnabled(true)

        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    // 신규 유저: 닉네임 화면으로
    private fun goNicknameFlow(
        provider: String,
        email: String,
        tempToken: String,
        providerAccess: String
    ) {
        val editor = secureSp.edit()
            .putString(KEY_SIGNUP_PROVIDER, provider)
            .putString(KEY_SIGNUP_TEMP_TOKEN, tempToken)
            .putString(KEY_SIGNUP_PROVIDER_ACCESS, providerAccess)

        if (email.isNotBlank()) {
            editor.putString(KEY_SIGNUP_EMAIL, email)
        } else {
            editor.remove(KEY_SIGNUP_EMAIL)
        }
        editor.commit()

        handlingCallback = false
        setButtonsEnabled(true)

        val i = Intent(this, NicknameActivity::class.java).apply {
            putExtra(EXTRA_SIGNUP_PROVIDER, provider)
            putExtra(EXTRA_SIGNUP_TEMP_TOKEN, tempToken)
            putExtra(EXTRA_SIGNUP_PROVIDER_ACCESS, providerAccess)
            if (email.isNotBlank()) putExtra(EXTRA_SIGNUP_EMAIL, email)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(i)
        finish()
    }

    private fun saveTokens(
        access: String,
        refresh: String,
        provider: String,
        email: String?,
        nick: String?,
        userId: Long?
    ) {
        secureSp.edit().apply {
            putString(ApiConfig.KEY_ACCESS, access)
            putString(ApiConfig.KEY_REFRESH, refresh)
            putString(ApiConfig.KEY_PROVIDER, provider)
            if (!email.isNullOrBlank()) putString(ApiConfig.KEY_EMAIL, email)
            if (!nick.isNullOrBlank()) putString(ApiConfig.KEY_NICKNAME, nick)
            if (userId != null && userId > 0) putLong(ApiConfig.KEY_USER_ID, userId)
            apply()
        }
    }

    private fun mirrorRefreshForAuthenticator(refresh: String) {
        if (refresh.isBlank()) return
        val esp = securePrefs("auth_prefs_secure")
        esp.edit().putString("refresh_token", refresh).apply()
        Log.d(TAG, "refresh_token mirrored to auth_prefs_secure")
    }

    private fun clearPendingFlow() {
        flowSp.edit().remove(KEY_PENDING_PROVIDER).remove(KEY_PENDING_STATE).apply()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnGoogle.isEnabled = enabled
        binding.btnKakao.isEnabled = enabled
    }

    private fun securePrefs(fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            this,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 기본값
    private fun securePrefs(): SharedPreferences = securePrefs(ApiConfig.PREFS_USER)

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

    private fun Response<*>.errorText(): String =
        runCatching { errorBody()?.string().orEmpty() }.getOrDefault("")

    companion object {
        private const val TAG = "앱체크"

        private const val KEY_PENDING_PROVIDER = "pendingProvider"
        private const val KEY_PENDING_STATE = "pendingState"

        private const val KEY_SIGNUP_PROVIDER = "signup_provider"
        private const val KEY_SIGNUP_EMAIL = "signup_email"
        private const val KEY_SIGNUP_TEMP_TOKEN = "signup_temp_token"
        private const val KEY_SIGNUP_PROVIDER_ACCESS = "signup_provider_access"

        const val EXTRA_SIGNUP_PROVIDER = "extra_signup_provider"
        const val EXTRA_SIGNUP_EMAIL = "extra_signup_email"
        const val EXTRA_SIGNUP_TEMP_TOKEN = "extra_signup_temp_token"
        const val EXTRA_SIGNUP_PROVIDER_ACCESS = "extra_signup_provider_access"
    }
}
