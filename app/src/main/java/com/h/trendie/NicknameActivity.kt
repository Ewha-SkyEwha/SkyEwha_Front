package com.h.trendie

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.data.auth.GoogleSignupReq
import com.h.trendie.data.auth.KakaoSignupReq
import com.h.trendie.network.ApiClient
import com.h.trendie.ApiConfig
import com.h.trendie.util.NicknameStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Response

class NicknameActivity : AppCompatActivity() {

    private val api by lazy { ApiClient.apiService }
    private val secureSp by lazy { securePrefs() }

    private lateinit var edtNickname: EditText
    private lateinit var btnSubmit: Button

    @Volatile
    private var submitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        persistSignupIfCameViaIntent()

        val root = findViewById<View>(R.id.root)
        val defaultBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (imeInsets.bottom > 0) v.updatePadding(bottom = imeInsets.bottom)
            else v.updatePadding(bottom = sysInsets.bottom + defaultBottom)
            insets
        }

        edtNickname = findViewById(R.id.editNickname)
        btnSubmit = findViewById(R.id.btnNextFromNickname)

        edtNickname.addTextChangedListener(SimpleTextWatcher { updateSubmitEnabled() })
        btnSubmit.setOnClickListener { submitSignup() }

        updateSubmitEnabled()
    }

    private fun updateSubmitEnabled() {
        btnSubmit.isEnabled = !submitting && !edtNickname.text.isNullOrBlank()
        if (edtNickname.text.isNullOrEmpty()) {
            edtNickname.error = null
        }
    }

    private fun submitSignup() {
        val provider = secureSp.getString(KEY_SIGNUP_PROVIDER, null)?.lowercase()
        val email = secureSp.getString(KEY_SIGNUP_EMAIL, null)
        val tempTok = secureSp.getString(KEY_SIGNUP_TEMP_TOKEN, null)
        val provAcc = secureSp.getString(KEY_SIGNUP_PROVIDER_ACCESS, null)

        Log.d(TAG, "가입체크 provider=$provider, email=$email, tempTok=$tempTok, provAcc=$provAcc")

        val needEmail = provider == "google"
        if (provider.isNullOrBlank() || tempTok.isNullOrBlank() || provAcc.isNullOrBlank()
            || (needEmail && email.isNullOrBlank())
        ) {
            bailToLogin("로그인 정보가 만료되었습니다. 다시 로그인해 주세요.")
            return
        }

        val nickname = edtNickname.text?.toString()?.trim().orEmpty()
        if (nickname.isEmpty()) {
            edtNickname.error = null
            toast("1~10자 사이로 닉네임을 입력해 주세요")
            return
        }
        val finalName = nickname

        submitting = true
        updateSubmitEnabled()
        Log.d(TAG, "[$provider] /signup 요청 nickname=$nickname, email=$email")

        lifecycleScope.launch {
            try {
                val res = withTimeout(12_000) {
                    withContext(Dispatchers.IO) {
                        when (provider) {
                            "google" -> api.googleSignup(
                                GoogleSignupReq(
                                    nickname = nickname,
                                    tempToken = tempTok,
                                    name = finalName,
                                    email = email!!,
                                    accessToken = provAcc
                                )
                            )

                            else -> api.kakaoSignup(
                                KakaoSignupReq(
                                    nickname = nickname,
                                    tempToken = tempTok,
                                    name = finalName,
                                    email = email.orEmpty(),
                                    accessToken = provAcc
                                )
                            )
                        }
                    }
                }

                if (!res.isSuccessful || res.body() == null) {
                    val code = res.code()
                    val body = res.errorText()
                    Log.e(TAG, "signup failed code=$code body=$body")
                    val msg = when (code) {
                        401, 403 -> "인증이 만료되었어요. 처음부터 다시 시도해 주세요."
                        409 -> "이미 가입된 계정이에요. 로그인으로 진행해 주세요."
                        422 -> "입력값 확인이 필요해요. 닉네임을 다시 확인해 주세요."
                        else -> "회원가입 실패($code)"
                    }
                    submitting = false
                    toast(msg)
                    updateSubmitEnabled()
                    return@launch
                }

                val body = res.body()!!
                val rawAccess = body.accessToken
                val refresh = body.refreshToken
                val user = body.user
                val emailV = user.email ?: email
                val userId = user.userId

                val pure = rawAccess
                    .removePrefix("Bearer ")
                    .removePrefix("bearer ")
                    .trim()

                if (pure.isBlank() || refresh.isBlank()) {
                    submitting = false
                    toast("토큰이 비어 있습니다. 잠시 후 다시 시도해 주세요")
                    updateSubmitEnabled()
                    return@launch
                }

                ApiClient.setJwt(pure)
                mirrorRefreshForAuthenticator(refresh)

                // 최종 토큰 + 유저 정보 저장
                secureSp.edit().apply {
                    putString(ApiConfig.KEY_ACCESS, pure)
                    putString(ApiConfig.KEY_REFRESH, refresh)
                    putString(ApiConfig.KEY_PROVIDER, provider)
                    if (!emailV.isNullOrBlank()) putString(ApiConfig.KEY_EMAIL, emailV)
                    putString(ApiConfig.KEY_NICKNAME, nickname)
                    if (userId != null && userId > 0) {
                        putLong(ApiConfig.KEY_USER_ID, userId)
                    }
                    apply()
                }

                NicknameStore.set(this@NicknameActivity, nickname)
                UserPrefs.setNickname(this@NicknameActivity, nickname)
                NicknameBus.emit(nickname)

                clearSignupTemps()

                toast("회원가입 완료!")

                startActivity(
                    Intent(this@NicknameActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()

            } catch (e: Exception) {
                submitting = false
                Log.e(TAG, "[$provider] /signup 실패", e)
                toast("네트워크 오류")
                updateSubmitEnabled()
            }
        }
    }

    private fun persistSignupIfCameViaIntent() {
        val p = intent.getStringExtra(LoginActivity.EXTRA_SIGNUP_PROVIDER)
        val e = intent.getStringExtra(LoginActivity.EXTRA_SIGNUP_EMAIL)
        val t = intent.getStringExtra(LoginActivity.EXTRA_SIGNUP_TEMP_TOKEN)
        val pa = intent.getStringExtra(LoginActivity.EXTRA_SIGNUP_PROVIDER_ACCESS)

        if (!p.isNullOrBlank() && !t.isNullOrBlank() && !pa.isNullOrBlank()) {
            val editor = secureSp.edit()
                .putString(KEY_SIGNUP_PROVIDER, p)
                .putString(KEY_SIGNUP_TEMP_TOKEN, t)
                .putString(KEY_SIGNUP_PROVIDER_ACCESS, pa)

            if (!e.isNullOrBlank()) {
                editor.putString(KEY_SIGNUP_EMAIL, e)
            } else {
                editor.remove(KEY_SIGNUP_EMAIL)
            }

            editor.commit()
            Log.d(TAG, "persisted from intent: provider=$p email=$e")
        }
    }

    private fun clearSignupTemps() {
        secureSp.edit().apply {
            remove(KEY_SIGNUP_PROVIDER)
            remove(KEY_SIGNUP_EMAIL)
            remove(KEY_SIGNUP_TEMP_TOKEN)
            remove(KEY_SIGNUP_PROVIDER_ACCESS)
            apply()
        }
    }

    private fun mirrorRefreshForAuthenticator(refresh: String) {
        if (refresh.isBlank()) return
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val esp = EncryptedSharedPreferences.create(
            this,
            "auth_prefs_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        esp.edit().putString("refresh_token", refresh).apply()
    }

    private fun bailToLogin(msg: String) {
        toast(msg)
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

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

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun Response<*>.errorText(): String =
        runCatching { errorBody()?.string().orEmpty() }.getOrDefault("")

    companion object {
        private const val TAG = "앱체크"
        private const val KEY_SIGNUP_PROVIDER = "signup_provider"
        private const val KEY_SIGNUP_EMAIL = "signup_email"
        private const val KEY_SIGNUP_TEMP_TOKEN = "signup_temp_token"
        private const val KEY_SIGNUP_PROVIDER_ACCESS = "signup_provider_access"
    }
}

private class SimpleTextWatcher(val onChange: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChange()
    override fun afterTextChanged(s: android.text.Editable?) {}
}
