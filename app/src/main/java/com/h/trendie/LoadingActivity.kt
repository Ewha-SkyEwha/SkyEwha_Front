package com.h.trendie

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.h.trendie.network.ApiClient
import com.h.trendie.data.ProcessVideoKeywordsReq
import kotlinx.coroutines.*

class LoadingActivity : AppCompatActivity() {

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        findViewById<TextView>(R.id.tvLoading)?.apply {
            isVisible = true
            text = "추천을 준비 중입니다…"
        }

        val feedbackIdFromIntent = intent.longExtraFlexible("feedback_id")
        val videoId              = intent.longExtraFlexible("video_id") ?: -1L
        val nickname             = intent.getStringExtra("nickname").orEmpty()
        val videoTitle           = intent.getStringExtra("videoTitle")
            ?: intent.getStringExtra("video_title").orEmpty()

        if ((feedbackIdFromIntent ?: -1L) > 0L) {
            job = lifecycleScope.launch {
                val feedbackId = feedbackIdFromIntent!!

                val report = pollReport(feedbackId)

                if (report == null) {
                    Toast.makeText(
                        this@LoadingActivity,
                        "추천 결과가 아직 준비되지 않았습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                goReport(
                    feedbackId = feedbackId,
                    videoId = videoId,
                    nickname = nickname,
                    videoTitle = videoTitle.ifBlank { "피드백" },
                    titles = report.titles,
                    hashtags = report.hashtags,
                    thumbs = report.thumbs
                )
                finish()
            }
            return
        }

        if (videoId <= 0L) {
            Toast.makeText(this, "영상 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        job = lifecycleScope.launch {
            val feedbackId = requestFeedbackIdOnce(videoId)
            if (feedbackId == null) {
                Toast.makeText(
                    this@LoadingActivity,
                    "추천 작업을 시작하지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            val report = pollReport(feedbackId)
            if (report == null) {
                Toast.makeText(
                    this@LoadingActivity,
                    "추천 결과가 아직 준비되지 않았습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            goReport(
                feedbackId = feedbackId,
                videoId = videoId,
                nickname = nickname,
                videoTitle = videoTitle.ifBlank { "피드백" },
                titles = report.titles,
                hashtags = report.hashtags,
                thumbs = report.thumbs
            )
            finish()
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private suspend fun requestFeedbackIdOnce(videoId: Long): Long? = withContext(Dispatchers.IO) {
        val resp = runCatching {
            ApiClient.apiService.processVideoKeywords(ProcessVideoKeywordsReq(videoId = videoId))
        }.getOrNull()

        if (resp == null || !resp.isSuccessful) return@withContext null

        // 서버는 { "feedback_id": 13 } 형태 (int/str 모두 대비)
        val raw = resp.body()?.feedbackId
        return@withContext raw.toLongLenient()
    }

    /**
     * 피드백 결과 폴링
     */
    private suspend fun pollReport(feedbackId: Long): ReportResult? = withContext(Dispatchers.IO) {
        val deadlineMs = System.currentTimeMillis() + 40_000L   // 최대 40초
        var wait = 600L
        var enoughTitlesOnce = false
        var lastNonEmpty: ReportResult? = null

        while (System.currentTimeMillis() < deadlineMs) {
            val resp = runCatching { ApiClient.apiService.getFeedback(feedbackId) }.getOrNull()

            if (resp != null && resp.isSuccessful) {
                val body = resp.body()

                val titles = body?.titles.orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val tags = body?.hashtags.orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val thumbs = body?.similarVideos.orEmpty().mapNotNull { sv ->
                    sv.thumbnailUrl ?: sv.videoUrl?.let { url ->
                        youtubeIdFromUrl(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
                    }
                }

                if (titles.isNotEmpty() || tags.isNotEmpty() || thumbs.isNotEmpty()) {
                    lastNonEmpty = ReportResult(titles, tags, thumbs)
                }

                val titlesDone = titles.size >= 5

                if (titlesDone) {
                    if (enoughTitlesOnce) {
                        return@withContext ReportResult(titles, tags, thumbs)
                    } else {
                        enoughTitlesOnce = true
                    }
                }
            }

            val now = System.currentTimeMillis()
            if (now + wait > deadlineMs) break

            delay(wait)
            if (wait < 4_000L) wait *= 2 else wait = 4_000L
        }

        return@withContext lastNonEmpty
    }

    private fun goReport(
        feedbackId: Long,
        videoId: Long,
        nickname: String,
        videoTitle: String,
        titles: List<String>,
        hashtags: List<String>,
        thumbs: List<String>
    ) {
        startActivity(
            Intent(this, FeedbackReportActivity::class.java).apply {
                if (videoId > 0L) putExtra("video_id", videoId)
                putExtra("feedback_id", feedbackId)
                putExtra("nickname", nickname)
                putExtra("videoTitle", videoTitle)
                putStringArrayListExtra("titles_fixed", ArrayList(titles))
                putStringArrayListExtra("hashtags_ready", ArrayList(hashtags))
                putStringArrayListExtra("thumbs_ready", ArrayList(thumbs))
            }
        )
    }

    // ---- util ----
    private fun Intent.longExtraFlexible(key: String): Long? {
        if (hasExtra(key)) {
            when (val v = extras?.get(key)) {
                is Long -> return v
                is Int -> return v.toLong()
                is Number -> return v.toLong()
                is String -> return v.trim().toLongOrNull()
                is CharSequence -> return v.toString().trim().toLongOrNull()
            }
        }
        return null
    }

    private fun Any?.toLongLenient(): Long? = when (this) {
        null -> null
        is Long -> this
        is Int -> this.toLong()
        is Number -> this.toLong()
        is String -> this.trim().toLongOrNull()
        is CharSequence -> this.toString().trim().toLongOrNull()
        else -> null
    }

    private fun youtubeIdFromUrl(url: String): String? {
        val patterns = listOf(
            "youtu\\.be/([A-Za-z0-9_-]{6,})",
            "youtube\\.com/watch\\?v=([A-Za-z0-9_-]{6,})",
            "youtube\\.com/shorts/([A-Za-z0-9_-]{6,})",
            "youtube\\.com/embed/([A-Za-z0-9_-]{6,})"
        )
        for (p in patterns) Regex(p).find(url)?.let { return it.groupValues[1] }
        return null
    }

    private data class ReportResult(
        val titles: List<String>,
        val hashtags: List<String>,
        val thumbs: List<String>
    )
}
