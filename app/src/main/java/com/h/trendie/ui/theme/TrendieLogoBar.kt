package com.h.trendie.ui.theme

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.h.trendie.R

class TrendieLogoBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_trendie_logo_bar, this, true)

        // 상태바(노치/펀치홀 포함) 높이만큼 자동 패딩 → 잘림/터치 막힘 방지
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = top + v.paddingTop)
            insets
        }
        requestApplyInsets()
    }
}
