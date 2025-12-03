package com.h.trendie.ui

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar

fun AppCompatActivity.setupBackToolbar(@IdRes toolbarId: Int, titleText: String? = null) {
    val tb = findViewById<MaterialToolbar>(toolbarId) ?: return
    tb.menu.clear()
    if (!titleText.isNullOrBlank()) tb.title = titleText
    tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

    // 상태바 인셋 자동 반영
    ViewCompat.setOnApplyWindowInsetsListener(tb) { v, insets ->
        val top = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        ).top
        v.updatePadding(top = top)
        insets
    }
}