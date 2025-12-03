package com.h.trendie

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.h.trendie.bookmark.BookmarkVideoAdapter
import com.h.trendie.bookmark.BookmarkViewModel
import com.h.trendie.bookmark.VideoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookmarkActivity : AppCompatActivity() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var empty: View

    private val vm: BookmarkViewModel by viewModels()

    private val videoAdapter = BookmarkVideoAdapter(
        onClick = { item -> openYoutube(item) },
        onUnbookmark = { _, pos -> vm.unbookmarkAt(pos) }
    )

    private var collectors: MutableList<Job> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        findViewById<MaterialToolbar?>(R.id.bookmarkToolbar)?.apply {
            menu.clear()
            title = "북마크"
            setNavigationIcon(R.drawable.backbutton)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        swipe = findViewById(R.id.swipe)
        rv = findViewById(R.id.rvBookmark)
        empty = findViewById(R.id.emptyState)

        rv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv.adapter = videoAdapter

        rv.setPadding(
            resources.getDimensionPixelSize(R.dimen.bookmark_list_hpad),
            0,
            resources.getDimensionPixelSize(R.dimen.bookmark_list_hpad),
            resources.getDimensionPixelSize(R.dimen.bookmark_list_bpad)
        )
        rv.clipToPadding = false

        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private val space = resources.getDimensionPixelSize(R.dimen.bookmark_list_vgap)
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val pos = parent.getChildAdapterPosition(view)
                outRect.top = if (pos == 0) 0 else space
            }
        })

        swipe.setOnRefreshListener { vm.refreshVideos() }

        collectStates()
        vm.refreshVideos()
    }

    private fun openYoutube(item: VideoItem) {
        val videoId = item.id
        if (videoId.isBlank()) {
            Toast.makeText(this, "영상 ID가 없어서 열 수 없어요.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://www.youtube.com/watch?v=$videoId"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "영상을 열 수 있는 앱이 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectStates() {
        collectors.forEach { it.cancel() }
        collectors.clear()

        collectors += lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 영상 목록
                launch {
                    vm.videos.collect { items -> renderVideos(items) }
                }
                launch {
                    vm.loading.collect { loading -> swipe.isRefreshing = loading }
                }
                // 에러 메시지
                launch {
                    vm.error.collect { err ->
                        if (err.isNullOrBlank()) return@collect

                        val lower = err.lowercase()
                        val msg: String? = when {
                            // 401 → 로그인 만료 안내
                            err.contains("401") || lower.contains("unauthorized") ->
                                "로그인이 만료되었어요. 다시 로그인해 주세요"

                            // 타임아웃/네트워크 계열
                            lower.contains("timeout") || lower.contains("failed to connect") ||
                                    lower.contains("unable to resolve host") ->
                                "네트워크가 불안정해요. 잠시 후 다시 시도해 주세요."

                            else -> null
                        }

                        msg?.let {
                            Toast.makeText(this@BookmarkActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderVideos(items: List<VideoItem>) {
        videoAdapter.submitList(items)
        toggleEmpty(items.isEmpty())
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        swipe.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}