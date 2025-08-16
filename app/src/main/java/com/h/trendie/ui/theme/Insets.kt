package com.h.trendie.ui.theme

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applyTopInsetPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.updatePadding(top = v.paddingTop + top)
        insets
    }
    requestApplyInsets()
}