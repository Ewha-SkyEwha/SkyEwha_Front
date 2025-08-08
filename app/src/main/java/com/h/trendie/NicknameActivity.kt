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
        val nextButton = findViewById<Button>(R.id.btnNextFromNickname)
        val tvError = findViewById<TextView>(R.id.tvNicknameError)

        editNickname.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val nickname = s.toString()
                val isValid = nickname.length in 2..10 && nickname.matches(Regex("^[a-zA-Z0-9가-힣]+$"))
                nextButton.isEnabled = isValid
                tvError.text = if (nickname.isEmpty() || isValid) "" else "닉네임 조건에 맞게 입력해주세요."
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val email = intent.getStringExtra("email") ?: ""
        val tempToken = intent.getStringExtra("tempToken") ?: ""
        val kakaoAccessToken = intent.getStringExtra("kakaoAccessToken") ?: ""
        val googleAccessToken = intent.getStringExtra("googleAccessToken") ?: ""

        // 로그인 타입
        val isGoogle = googleAccessToken.isNotEmpty()
        val isKakao = kakaoAccessToken.isNotEmpty()

        nextButton.setOnClickListener {
            val nickname = editNickname.text.toString()
            if (isGoogle) {
                registerGoogleUser(nickname, email, tempToken, googleAccessToken)
            } else {
                registerKakaoUser(nickname, email, tempToken, kakaoAccessToken)
            }
        }
    }

    // 카카오 신규회원
    private fun registerKakaoUser(nickname: String, email: String, tempToken: String, kakaoAccessToken: String) {
        val url = "http://서버주소/api/v1/auth/kakao/signup"
        val json = JSONObject().apply {
            put("nickname", nickname)
            put("email", email)
            put("tempToken", tempToken)
            put("kakao_access_token", kakaoAccessToken)
        }
        sendSignupRequest(url, json, nickname)
    }

    // 구글 신규회원
    private fun registerGoogleUser(nickname: String, email: String, tempToken: String, googleAccessToken: String) {
        val url = "http://서버주소/api/v1/auth/google/signup"
        val json = JSONObject().apply {
            put("nickname", nickname)
            put("email", email)
            put("tempToken", tempToken)
            put("google_access_token", googleAccessToken)
        }
        sendSignupRequest(url, json, nickname)
    }

    // 회원가입 공통 처리
    private fun sendSignupRequest(url: String, json: JSONObject, nickname: String) {
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("회원가입 에러", "Code: ${response.code}, body: $resStr")
                    runOnUiThread {
                        Toast.makeText(this@NicknameActivity, "회원가입 실패: 서버 오류", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                Log.d("회원가입 응답", resStr)
                try {
                    val jsonResponse = JSONObject(resStr)
                    val accessToken = jsonResponse.optString("accessToken", "")
                    val refreshToken = jsonResponse.optString("refreshToken", "")
                    saveAuthToken(accessToken, refreshToken)

                    runOnUiThread {
                        val intent = Intent(this@NicknameActivity, PreferenceActivity::class.java)
                        intent.putExtra("nickname", nickname)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("회원가입 파싱실패", "Malformed JSON: $resStr")
                    runOnUiThread {
                        Toast.makeText(this@NicknameActivity, "잘못된 서버 응답!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@NicknameActivity, "회원가입 실패: 네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun saveAuthToken(accessToken: String, refreshToken: String) {
        if (accessToken.isEmpty()) return
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("accessToken", accessToken)
            putString("refreshToken", refreshToken)
            apply()
        }
        Log.d("TokenCheck", "Saved accessToken: $accessToken, refreshToken: $refreshToken")
    }
}