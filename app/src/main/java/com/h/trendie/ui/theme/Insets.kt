package com.h.trendie.ui.theme

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// 상태바 높이를 상단 패딩에 더해 주되 누적 X
fun View.applyTopInsetPadding() {
    val tagKey = com.h.trendie.R.id.tag_base_padding_top
    val baseTop = (getTag(tagKey) as? Int) ?: paddingTop.also { setTag(tagKey, it) }

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.updatePadding(top = baseTop + topInset)
        insets
    }
    // 최초 1회 강제 분배
    requestApplyInsets()
}