package com.h.trendie

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.h.trendie.util.UserPrefs

class MemberInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_info)

        // 상단 툴바 타이틀 & 뒤로가기 (view_simple_toolbar 포함 레이아웃에 맞춤)
        setupSimpleToolbar(R.string.title_member_info)

        // ✅ 저장된 닉네임/이메일을 화면에 표시
        findViewById<TextView>(R.id.tvNickname)?.text =
            UserPrefs.getNickname(this)   // 없으면 내부에서 "유저"로 처리했다면 그대로 사용

        findViewById<TextView>(R.id.tvEmail)?.text =
            UserPrefs.getEmail(this) ?: getString(R.string.label_no_linked_account)
    }
}