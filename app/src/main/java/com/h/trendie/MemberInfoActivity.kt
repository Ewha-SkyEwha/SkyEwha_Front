package com.h.trendie

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MemberInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        findViewById<TextView>(R.id.tvTitle)?.text = getString(R.string.title_member_info)
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_info)
        setupSimpleToolbar(R.string.title_settings)
    }
}