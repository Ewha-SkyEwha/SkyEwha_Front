package com.h.trendie

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.model.PresearchReq
import com.h.trendie.model.PresearchRes
import com.h.trendie.model.VideoSearchResponse
import com.h.trendie.network.ApiClient
import com.h.trendie.network.BookmarkedVideoDto
import com.h.trendie.ui.theme.applyTopInsetPadding
import kotlinx.coroutines.*

class KeywordResultActivity : AppCompatActivity() {

    private val scope = MainScope()

    private lateinit var tvKeyword: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rv: RecyclerView

    private val bookmarkedIds = mutableSetOf<String>()

    private val inFlight = mutableSetOf<String>()

    private lateinit var adapter: KeywordResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyword_result)

        tvKeyword = findViewById(R.id.tvKeywordTitle)
        tvEmpty   = findViewById(R.id.tvEmpty)
        rv        = findViewById(R.id.rvResults)

        findViewById<View>(R.id.logoSpacer)?.applyTopInsetPadding()

        ViewCompat.setOnApplyWindowInsetsListener(rv) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = sys.bottom.coerceAtLeast(v.paddingBottom))
            insets
        }

        adapter = KeywordResultAdapter(
            onItemClick = { item ->
                val url = item.videoUrl ?: "https://www.youtube.com/watch?v=${item.videoId}"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            onBookmarkClick = { item, position ->
                val id = item.videoId
                if (!inFlight.add(id)) return@KeywordResultAdapter
                if (bookmarkedIds.contains(id)) removeBookmark(id, position)
                else addBookmark(id, position)
            },
            isBookmarked = { vid -> bookmarkedIds.contains(vid) }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
        rv.adapter = adapter
        rv.itemAnimator = null
        rv.clipToPadding = false

        val keywords = intent.getStringArrayListExtra(EXTRA_KEYWORDS).orEmpty()
        tvKeyword.text = if (keywords.isNotEmpty()) "# " + keywords.joinToString(", ") else "# (키워드 없음)"

        if (keywords.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
            return
        }

        // === 북마크목록 + 검색결과 병렬 로딩 ===
        scope.launch {
            val bookmarksDeferred: Deferred<List<String>> = async(Dispatchers.IO) {
                try {
                    val list: List<BookmarkedVideoDto> = ApiClient.apiService.getMyBookmarkedVideos()
                    list.map { it.video_id }.filter { it.isNotBlank() }
                } catch (_: Exception) { emptyList() }
            }

            val resultsDeferred: Deferred<List<VideoSearchResponse>> = async(Dispatchers.IO) {
                try {
                    val resp = ApiClient.apiService.presearch(PresearchReq(keywords = keywords))
                    if (!resp.isSuccessful) return@async emptyList<VideoSearchResponse>()
                    val body: PresearchRes? = resp.body()
                    body?.results.orEmpty()
                } catch (_: Exception) { emptyList() }
            }

            bookmarkedIds.clear()
            bookmarkedIds.addAll(bookmarksDeferred.await())

            val items = resultsDeferred.await()
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(items)
        }
    }

    /** 북마크 추가 */
    private fun addBookmark(videoId: String, position: Int) {
        scope.launch {
            val tag = "BookmarkAdd"
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.addBookmark(videoId)
                }
                android.util.Log.d(tag, "code=${resp.code()} body=${resp.body()} err=${resp.errorBody()?.string()}")
                val ok = resp.isSuccessful || resp.code() == 409
                if (ok) {
                    bookmarkedIds.add(videoId)
                    adapter.notifyItemChanged(position, "bookmark")
                    Toast.makeText(this@KeywordResultActivity, "북마크에 추가했어요", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@KeywordResultActivity,
                        "북마크 실패",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e(tag, "exception=${e.message}", e)
                Toast.makeText(this@KeywordResultActivity, "북마크 실패", Toast.LENGTH_LONG).show()
            } finally {
                inFlight.remove(videoId)
            }
        }
    }

    private fun removeBookmark(videoId: String, position: Int) {
        scope.launch {
            val tag = "BookmarkDel"
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.unbookmark(videoId)
                }
                android.util.Log.d(tag, "code=${resp.code()} body=${resp.body()} err=${resp.errorBody()?.string()}")
                val ok = resp.isSuccessful || resp.code() == 404  // 없어도 성공 취급
                if (ok) {
                    bookmarkedIds.remove(videoId)
                    adapter.notifyItemChanged(position, "bookmark")
                    Toast.makeText(this@KeywordResultActivity, "북마크를 해제했어요", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@KeywordResultActivity,
                        "북마크 해제 실패",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e(tag, "exception=${e.message}", e)
                Toast.makeText(this@KeywordResultActivity, "북마크 해제 실패 (예외)", Toast.LENGTH_LONG).show()
            } finally {
                inFlight.remove(videoId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val EXTRA_KEYWORDS = "extra_keywords"
    }
}
