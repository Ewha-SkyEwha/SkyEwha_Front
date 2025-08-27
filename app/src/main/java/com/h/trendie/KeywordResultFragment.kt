package com.h.trendie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.model.PresearchReq
import com.h.trendie.model.VideoItem
import com.h.trendie.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeywordResultFragment : Fragment(R.layout.fragment_keyword_result) {

    private lateinit var tvKeyword: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rv: RecyclerView
    private val adapter = KeywordResultAdapter { item ->
        item.videoUrl?.let { startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))
        )}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvKeyword = view.findViewById(R.id.tvKeywordTitle)
        tvEmpty   = view.findViewById(R.id.tvEmpty)
        rv        = view.findViewById(R.id.rvResults)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = v.paddingBottom + b); insets
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.itemAnimator = null

        val keyword = requireArguments().getString(ARG_KEYWORD).orEmpty()
        tvKeyword.text = "# $keyword"

        // 실제 API 호출
        viewLifecycleOwner.lifecycleScope.launch {
            val videos: List<VideoItem> = try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.youtubeApi.presearch(PresearchReq(keywords = listOf(keyword)))
                }
                if (resp.isSuccessful) resp.body()?.items.orEmpty() else emptyList()
            } catch (_: Exception) { emptyList() }

            tvEmpty.isVisible = videos.isEmpty()
            rv.isVisible = videos.isNotEmpty()
            adapter.submit(videos)
        }
    }

    companion object {
        private const val ARG_KEYWORD = "arg_keyword"
        fun newInstance(keyword: String) =
            KeywordResultFragment().apply {
                arguments = bundleOf(ARG_KEYWORD to keyword)
            }
    }
}