package com.h.trendie

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NicknameEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname_edit)
        setupSimpleToolbar(R.string.title_settings)

        val editText = findViewById<EditText>(R.id.editNickname)
        val btnSave = findViewById<Button>(R.id.btnSaveNickname)

        val current = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("nickname", "")

        editText.setText(current)

        btnSave.setOnClickListener {
            val newNickname = editText.text.toString()
            if (newNickname.length in 2..10) {
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .edit().putString("nickname", newNickname).apply()
                Toast.makeText(this, "닉네임이 변경되었습니다!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "2~10자의 닉네임만 가능합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}