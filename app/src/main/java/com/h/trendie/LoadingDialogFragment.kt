package com.h.trendie

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment

class LoadingDialogFragment : DialogFragment(R.layout.fragment_loading) {
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // 풀스크린 투명 배경
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        isCancelable = false
    }
}
