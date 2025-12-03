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
        LayoutInflater.from(context).inflate(R.layout.include_trendie_header, this, true)

        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val baseTop = (v.getTag(R.id.tag_initial_padding_top) as? Int) ?: v.paddingTop.also {
                v.setTag(R.id.tag_initial_padding_top, it)
            }   //패딩 누적 방지
            v.updatePadding(top = baseTop + topInset)
            insets
        }
        requestApplyInsets()
    }
}
