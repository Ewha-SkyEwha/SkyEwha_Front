package com.h.trendie

import android.view.*
import androidx.fragment.app.DialogFragment

class LoadingDialogFragment : DialogFragment(R.layout.activity_loading) {
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        isCancelable = false
    }
}
