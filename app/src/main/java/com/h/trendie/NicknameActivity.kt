package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class NicknameActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        val editNickname = findViewById<EditText>(R.id.editNickname)
        val nextButton   = findViewById<Button>(R.id.btnNextFromNickname)
        val tvError      = findViewById<TextView>(R.id.tvNicknameError)

        // 유효성
        editNickname.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val nickname = s?.toString() ?: ""
                val isValid = nickname.length in 2..10 && nickname.matches(Regex("^[a-zA-Z0-9가-힣]+$"))
                nextButton.isEnabled = isValid
                tvError.text = if (nickname.isEmpty() || isValid) "" else "닉네임 조건에 맞게 입력해주세요."
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 전달받은 값
        val provider = intent.getStringExtra("provider") ?: "kakao" // "google" | "kakao"
        val email    = intent.getStringExtra("email") ?: ""
        val tempToken = intent.getStringExtra("tempToken") ?: ""
        val providerAccessToken = intent.getStringExtra("providerAccessToken") ?: ""

        nextButton.setOnClickListener {
            val nickname = editNickname.text.toString()
            registerUser(provider, nickname, email, tempToken, providerAccessToken)
        }
    }

    /** /{provider}/signup 호출 */
    private fun registerUser(
        provider: String,
        nickname: String,
        email: String,
        tempToken: String,
        providerAccessToken: String
    ) {
        val url = "${ApiConfig.BASE_URL}/api/v1/auth/$provider/signup"

        val json = JSONObject().apply {
            put("nickname", nickname)
            put("email", email)
            put("tempToken", tempToken)
            // 서버 명세에 맞춰 key 이름 분기
            put("${provider}_access_token", providerAccessToken) // kakao_access_token | google_access_token
        }

        val reqBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val req = Request.Builder().url(url).post(reqBody).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                Log.d("앱체크", "[$provider] /signup 응답: $resStr")

                if (!response.isSuccessful) {
                    runOnUiThread { Toast.makeText(this@NicknameActivity, "회원가입 실패(${response.code})", Toast.LENGTH_SHORT).show() }
                    return
                }

                try {
                    val jr = JSONObject(resStr)
                    val access  = jr.optString("accessToken", "")
                    val refresh = jr.optString("refreshToken", "")
                    if (access.isBlank()) throw IllegalStateException("accessToken 없음")

                    saveAuth(access, refresh, provider, email)
                    runOnUiThread {
                        val i = Intent(this@NicknameActivity, PreferenceActivity::class.java)
                        i.putExtra("nickname", nickname)
                        i.putExtra("email", email)
                        startActivity(i)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("앱체크", "[$provider] /signup 파싱 실패: $resStr", e)
                    runOnUiThread { Toast.makeText(this@NicknameActivity, "서버 응답 파싱 실패", Toast.LENGTH_SHORT).show() }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("앱체크", "[$provider] /signup onFailure: ${e.message}", e)
                runOnUiThread { Toast.makeText(this@NicknameActivity, "회원가입 네트워크 오류", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun saveAuth(access: String, refresh: String, provider: String, email: String?) {
        val sp = getSharedPreferences(ApiConfig.PREFS_USER, MODE_PRIVATE)
        sp.edit().apply {
            putString(ApiConfig.KEY_ACCESS, access)
            putString(ApiConfig.KEY_REFRESH, refresh)
            putString(ApiConfig.KEY_PROVIDER, provider)
            if (!email.isNullOrBlank()) putString(ApiConfig.KEY_EMAIL, email)
            apply()
        }
        Log.d("앱체크", "회원가입 토큰 저장 완료 provider=$provider")
    }
}