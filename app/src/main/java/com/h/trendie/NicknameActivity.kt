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
import com.h.trendie.data.UserPrefs
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class NicknameActivity : AppCompatActivity() {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val secureSp by lazy { securePrefs() }

    private lateinit var edtNickname: EditText
    private lateinit var btnSubmit: Button

    @Volatile private var submitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        val root = findViewById<View>(R.id.root) // 최상위 ConstraintLayout에 android:id="@+id/root"
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(bottom = max(v.paddingBottom, ime.bottom))
            insets
        }

        // ⬇️ 레이아웃의 실제 id에 맞춤
        edtNickname = findViewById(R.id.editNickname)
        btnSubmit   = findViewById(R.id.btnNextFromNickname)

        edtNickname.addTextChangedListener(SimpleTextWatcher {
            btnSubmit.isEnabled = !submitting && edtNickname.text?.isNotBlank() == true
        })

        btnSubmit.setOnClickListener {
            if (!submitting) submitSignup()
        }
        btnSubmit.isEnabled = edtNickname.text?.isNotBlank() == true
    }

    private fun submitSignup() {
        // LoginActivity에서 저장해 둔 임시값들
        val provider = secureSp.getString(KEY_SIGNUP_PROVIDER, null)
        val email    = secureSp.getString(KEY_SIGNUP_EMAIL, null)
        val tempTok  = secureSp.getString(KEY_SIGNUP_TEMP_TOKEN, null)
        val provAcc  = secureSp.getString(KEY_SIGNUP_PROVIDER_ACCESS, null)

        if (provider.isNullOrBlank() || email.isNullOrBlank() ||
            tempTok.isNullOrBlank() || provAcc.isNullOrBlank()
        ) {
            toast("가입 정보가 유효하지 않습니다. 처음부터 다시 시도해 주세요.")
            finish()
            return
        }

        val nickname = edtNickname.text?.toString()?.trim().orEmpty()
        if (nickname.isBlank()) {
            toast("닉네임을 입력해 주세요")
            return
        }
        val finalName = nickname // 닉네임을 이름으로도 사용

        val url = "${ApiConfig.BASE_URL}/api/v1/auth/$provider/signup"
        val payload = JSONObject().apply {
            put("nickname",     nickname)
            put("temp_token",   tempTok)
            put("name",         finalName)
            put("email",        email)
            put("access_token", provAcc)
        }.toString()

        val body = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(url).post(body).build()

        submitting = true
        btnSubmit.isEnabled = false
        Log.d(TAG, "[$provider] /signup 요청: ${safe(payload)}")

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string().orEmpty()
                Log.d(TAG, "[$provider] /signup 응답(${response.code}): ${safe(resStr)}")

                if (!response.isSuccessful) {
                    submitting = false
                    runOnUiThread {
                        toast("회원가입 실패(${response.code})")
                        btnSubmit.isEnabled = edtNickname.text?.isNotBlank() == true
                    }
                    return
                }

                try {
                    val json    = JSONObject(resStr)
                    val access  = json.optString("accessToken", "")
                    val refresh = json.optString("refreshToken", "")
                    val user    = json.optJSONObject("user")
                    val emailV  = user?.optString("email") ?: json.optString("email", "")

                    if (access.isBlank() || refresh.isBlank()) {
                        submitting = false
                        runOnUiThread {
                            toast("토큰이 비어 있습니다. 잠시 후 다시 시도해 주세요")
                            btnSubmit.isEnabled = edtNickname.text?.isNotBlank() == true
                        }
                        return
                    }

                    // 최종 토큰 저장
                    secureSp.edit().apply {
                        putString(ApiConfig.KEY_ACCESS, access)
                        putString(ApiConfig.KEY_REFRESH, refresh)
                        putString(ApiConfig.KEY_PROVIDER, provider)
                        if (emailV.isNotBlank()) putString(ApiConfig.KEY_EMAIL, emailV)
                        apply()
                    }

                    runOnUiThread {
                        lifecycleScope.launch {
                            // DataStore에 최종 닉네임 기록 (즉시 반영됨)
                            UserPrefs.setNickname(this@NicknameActivity, nickname)

                            toast("회원가입 완료!")
                            startActivity(Intent(this@NicknameActivity, MainActivity::class.java))

                            // 닉네임을 일반 SharedPreferences에도 저장 + 전역 알림 (즉시 반영)
                            getSharedPreferences("user_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("nickname", nickname)
                                .apply()
                            NicknameBus.emit(nickname)

                            // 임시값 제거
                            clearSignupTemps()
                            finish()
                        }
                    }


                } catch (e: Exception) {
                    submitting = false
                    Log.e(TAG, "[$provider] /signup 파싱 실패: ${safe(resStr)}", e)
                    runOnUiThread {
                        toast("응답 파싱 실패")
                        btnSubmit.isEnabled = edtNickname.text?.isNotBlank() == true
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                submitting = false
                Log.e(TAG, "[$provider] /signup onFailure: ${e.message}", e)
                runOnUiThread {
                    toast("네트워크 오류")
                    btnSubmit.isEnabled = edtNickname.text?.isNotBlank() == true
                }
            }
        })
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

    private fun safe(s: String?): String = s?.let { if (it.length > 300) it.take(300) + "..." else it } ?: ""

    companion object {
        private const val TAG = "앱체크"
        private const val KEY_SIGNUP_PROVIDER        = "signup_provider"
        private const val KEY_SIGNUP_EMAIL           = "signup_email"
        private const val KEY_SIGNUP_TEMP_TOKEN      = "signup_temp_token"
        private const val KEY_SIGNUP_PROVIDER_ACCESS = "signup_provider_access"
    }
}

// 간단한 텍스트워처
private class SimpleTextWatcher(val onChange: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChange()
    override fun afterTextChanged(s: android.text.Editable?) {}
}