package com.h.trendie

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.h.trendie.ui.theme.applyTopInsetPadding

fun Activity.setupSimpleToolbar(@StringRes titleRes: Int? = null) {
    // id 정적 참조
    val candidates = listOf(
        R.id.settingsToolbar,
        R.id.prefToolbar
    )

    var toolbar: View? = null
    for (id in candidates) {
        val v = findViewById<View?>(id)
        if (v != null) { toolbar = v; break }
    }

    // 동적 조회 보조
    if (toolbar == null) {
        val dynId = resources.getIdentifier("toolbar", "id", packageName)
        if (dynId != 0) toolbar = findViewById(dynId)
    }

    toolbar ?: return

    toolbar.applyTopInsetPadding()

    // 제목
    titleRes?.let { res ->
        toolbar.findViewById<TextView?>(R.id.tvTitle)?.setText(res)
    }

    // 뒤로가기
    toolbar.findViewById<View?>(R.id.btnBack)?.setOnClickListener {
        finish()
    }
}