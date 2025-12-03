package com.h.trendie.ui.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.os.bundleOf
import com.h.trendie.R
import com.h.trendie.FeedbackUploadFragment
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChooseInputMethodBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottomsheet_choose_input, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 카드 클릭 -> 결과 보내고 닫기
        view.findViewById<View>(R.id.cardVideo).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                FeedbackUploadFragment.REQ_CHOOSE_METHOD,
                bundleOf(FeedbackUploadFragment.KEY_CHOICE to FeedbackUploadFragment.CHOICE_VIDEO)
            )
            dismiss()
        }

        view.findViewById<View>(R.id.cardText).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                FeedbackUploadFragment.REQ_CHOOSE_METHOD,
                bundleOf(FeedbackUploadFragment.KEY_CHOICE to FeedbackUploadFragment.CHOICE_TEXT)
            )
            dismiss()
        }

        isCancelable = true
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(sheet!!)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    companion object {
        fun newInstance() = ChooseInputMethodBottomSheet()
    }
}
