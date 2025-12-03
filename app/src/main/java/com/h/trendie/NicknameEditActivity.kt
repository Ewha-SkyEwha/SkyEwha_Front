package com.h.trendie

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.h.trendie.network.ApiClient
import com.h.trendie.network.NicknameUpdateReq
import kotlinx.coroutines.launch

class NicknameEditActivity : AppCompatActivity() {

    private val api by lazy { ApiClient.apiService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname_edit)

        val editText = findViewById<EditText>(R.id.editNickname)
        val btnSave  = findViewById<Button>(R.id.btnSaveNickname)

        val userId = intent.getLongExtra("user_id", -1L)
        if (userId <= 0L) {
            Toast.makeText(this, "유저 정보를 불러올 수 없어요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val current = UserPrefs.getNickname(this@NicknameEditActivity)
            if (!current.isNullOrBlank()) editText.setText(current)
        }

        editText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnSave.performClick()
                true
            } else false
        }

        btnSave.setOnClickListener {
            val newNickname = editText.text.toString().trim()

            if (newNickname.length !in 1..10) {
                Toast.makeText(this, "1~10자의 닉네임만 가능합니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            val originalText = btnSave.text
            btnSave.text = "저장 중…"
            getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(editText.windowToken, 0)

            lifecycleScope.launch {
                try {
                    val res = api.updateNickname(userId, NicknameUpdateReq(newNickname))
                    if (res.isSuccessful) {
                        UserPrefs.setNickname(this@NicknameEditActivity, newNickname)
                        NicknameBus.emit(newNickname)

                        Toast.makeText(this@NicknameEditActivity, "닉네임이 변경되었습니다!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK, intent.putExtra("nickname", newNickname))
                        finish()
                    } else {
                        val msg = when (res.code()) {
                            400 -> "닉네임 형식이 올바르지 않습니다."
                            401 -> "다시 로그인해 주세요."
                            403 -> "권한이 없습니다."
                            409 -> "이미 사용 중인 닉네임입니다."
                            else -> "변경에 실패했습니다. (${res.code()})"
                        }
                        Toast.makeText(this@NicknameEditActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@NicknameEditActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                } finally {
                    btnSave.isEnabled = true
                    btnSave.text = originalText
                }
            }
        }
    }
}
