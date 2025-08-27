package com.h.trendie

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.button.MaterialButton

class KeywordFragment : Fragment(R.layout.fragment_keyword) {

    private lateinit var etKeyword: EditText
    private lateinit var btnSearch: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etKeyword = view.findViewById(R.id.etKeyword)
        btnSearch = view.findViewById(R.id.btnSearchKeyword)

        // IME/네비게이션 바 인셋 반영
        val root = view.findViewById<View>(R.id.rootKeyword)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val types = WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime()
            v.updatePadding(bottom = insets.getInsets(types).bottom)
            insets
        }

        fun goSearch() {
            val keyword = etKeyword.text?.toString()?.trim().orEmpty()
            if (keyword.isEmpty()) {
                Toast.makeText(requireContext(), "키워드를 입력하세요", Toast.LENGTH_SHORT).show()
                return
            }

            // 키보드 내려주기
            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(etKeyword.windowToken, 0)

            // 현재 Fragment 가 들어있는 부모 컨테이너 id 가져오기
            val containerId = (requireView().parent as? View)?.id
                ?: throw IllegalStateException("Container ID not found")

            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace(containerId, KeywordResultFragment.newInstance(keyword))
                addToBackStack("keyword_result")
            }
        }

        btnSearch.setOnClickListener { goSearch() }
        etKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                goSearch()
                true
            } else false
        }
    }
}