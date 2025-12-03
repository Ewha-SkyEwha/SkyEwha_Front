package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.fragment.app.Fragment
import com.h.trendie.ui.theme.applyTopInsetPadding
import kotlin.jvm.java

class KeywordFragment : Fragment(R.layout.fragment_keyword) {

    private lateinit var etKeyword: EditText
    private lateinit var btnSearch: View
    private var centerGuide: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etKeyword   = view.findViewById(R.id.etKeyword)
        btnSearch   = view.findViewById(R.id.btnSearchKeyword)
        centerGuide = view.findViewById(R.id.centerGuide)

        // 헤더 인셋
        view.findViewById<View>(R.id.header)?.applyTopInsetPadding()
        view.findViewById<View>(R.id.headerContainer)?.applyTopInsetPadding()

        // IME 있을 때만 버튼 올리기
        val baseBtnMargin = (btnSearch.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeVisible = insets.isVisible(Type.ime())
            val imeBottom  = insets.getInsets(Type.ime()).bottom

            centerGuide?.visibility = if (imeVisible) View.GONE else View.VISIBLE

            (btnSearch.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                val newMargin = if (imeVisible) baseBtnMargin + imeBottom else baseBtnMargin
                if (lp.bottomMargin != newMargin) {
                    lp.bottomMargin = newMargin
                    btnSearch.layoutParams = lp
                }
            }
            WindowInsetsCompat.CONSUMED
        }

        fun tokenize(input: String): List<String> =
            input.replace('，', ',')
                .split(",", "\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(10)

        fun goSearch() {
            val raw = etKeyword.text?.toString().orEmpty()
            val keywords = tokenize(raw)
            if (keywords.isEmpty()) {
                Toast.makeText(requireContext(), "키워드를 입력하세요", Toast.LENGTH_SHORT).show()
                return
            }

            // 키보드 내리기
            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(etKeyword.windowToken, 0)
            etKeyword.clearFocus()

            // 결과 화면 이동
            val intent = Intent(requireContext(), KeywordResultActivity::class.java).apply {
                putStringArrayListExtra(
                    KeywordResultActivity.EXTRA_KEYWORDS,
                    ArrayList(keywords)
                )
            }
            startActivity(intent)
        }

        btnSearch.setOnClickListener { goSearch() }
        etKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                goSearch(); true
            } else false
        }
    }
}
