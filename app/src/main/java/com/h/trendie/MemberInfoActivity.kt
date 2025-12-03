package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.network.ApiClient
import com.h.trendie.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemberInfoActivity : AppCompatActivity() {

    private val api: ApiService by lazy { ApiClient.apiService }

    private val secureSp by lazy {
        val key = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            this,
            ApiConfig.PREFS_USER,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_info)

        setupSimpleToolbar(R.string.title_member_info)

        // 제공자 확인
        val provider = secureSp.getString(ApiConfig.KEY_PROVIDER, null)?.lowercase()
        val email = UserPrefs.getEmailSync(this)

        val tvEmail = findViewById<TextView>(R.id.tvEmail)

        val linkedText = when (provider) {
            "google" -> {
                when {
                    !email.isNullOrBlank() -> "Google · $email"
                    else -> "Google 계정으로 로그인 중이에요"
                }
            }
            "kakao" -> {
                when {
                    !email.isNullOrBlank() -> "Kakao · $email"
                    else -> "카카오 계정으로 로그인 중이에요"
                }
            }
            else -> {
                email ?: getString(R.string.label_no_linked_account)
            }
        }

        tvEmail?.text = linkedText

        findViewById<android.view.View>(R.id.btnDeleteAccount)?.setOnClickListener {
            confirmAndUnlink()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val nickname = UserPrefs.getNickname(this@MemberInfoActivity)
            findViewById<TextView>(R.id.tvNickname)?.text = nickname
        }
    }

    private fun confirmAndUnlink() {
        val provider = secureSp.getString(ApiConfig.KEY_PROVIDER, null)
        val access   = secureSp.getString(ApiConfig.KEY_ACCESS, null)

        if (provider.isNullOrBlank() || access.isNullOrBlank()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
            return
        }

        val providerLabel = when (provider.lowercase()) {
            "kakao" -> "카카오"
            "google" -> "구글"
            else -> "계정"
        }

        AlertDialog.Builder(this)
            .setTitle("회원 탈퇴")
            .setMessage("$providerLabel 연결을 해제하고 회원 탈퇴하시겠어요?\n\n" +
                    "• 모든 토큰이 삭제되고\n• 앱의 내 로컬 데이터가 초기화됩니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("탈퇴") { _, _ -> unlink(provider, access) }
            .show()
    }

    private fun unlink(provider: String, access: String) {
        lifecycleScope.launch {
            ApiClient.setJwt(access)

            val res = withContext(Dispatchers.IO) {
                when (provider.lowercase()) {
                    "kakao"  -> api.kakaoUnlink()
                    "google" -> api.googleUnlink()
                    else     -> null
                }
            } ?: return@launch

            if (!res.isSuccessful) {
                Toast.makeText(
                    this@MemberInfoActivity,
                    if (res.code() == 403) "인증 만료. 다시 로그인해 주세요." else "탈퇴 실패",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val ok = when (res.code()) {
                200 -> res.body()?.success ?: true
                204 -> true
                else -> res.isSuccessful
            }

            if (!ok) {
                Toast.makeText(this@MemberInfoActivity, "탈퇴 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(this@MemberInfoActivity, "탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            clearAllAndGoLogin()
        }
    }

    private fun clearAllAndGoLogin() {
        secureSp.edit().clear().apply()
        lifecycleScope.launch { UserPrefs.clear(this@MemberInfoActivity) }

        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}