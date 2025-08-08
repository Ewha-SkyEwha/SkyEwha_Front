package com.h.trendie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class KeywordFragment : Fragment(R.layout.fragment_keyword) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_keyword, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etKeyword = view.findViewById<EditText>(R.id.etKeyword)
        val btnSearch = view.findViewById<Button>(R.id.btnSearchKeyword)

        btnSearch.setOnClickListener {
            val keyword = etKeyword.text.toString().trim()
            if (keyword.isNotEmpty()) {
                // 키워드 검색 로직 추가?
            } else {
                etKeyword.error = "키워드를 입력하세요"
            }
        }
    }
}