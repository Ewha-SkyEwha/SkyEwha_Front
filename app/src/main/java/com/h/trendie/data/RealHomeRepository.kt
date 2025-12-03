package com.h.trendie.data

import android.util.Log
import com.h.trendie.HomeRepository
import com.h.trendie.model.*
import com.h.trendie.network.ApiClient
import com.h.trendie.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

class RealHomeRepository(
    private val api: ApiService = ApiClient.apiService
) : HomeRepository {

    override suspend fun getSnapshot(): HomeSnapshot = withContext(Dispatchers.IO) {
        // 1) 주간 트렌드
        val weeklyRes = try { api.getWeeklyTrend() } catch (e: Exception) {
            Log.e(TAG, "getWeeklyTrend() call failed", e); null
        }

        val (hashtags, rising) = if (weeklyRes?.isSuccessful == true && weeklyRes.body() != null) {
            val body = weeklyRes.body()!!
            mapHashtags(body) to mapRising(body)
        } else {
            emptyList<HashtagRank>() to emptyList<RisingKeyword>()
        }

        // 2) 인기(트렌드) 동영상
        val videos: List<VideoItem> = try {
            val resp = api.getPopularVideos()
            if (resp.isSuccessful) {
                resp.body()?.results.orEmpty()
                    .map { dto ->
                        val id = dto.videoId
                        VideoItem(
                            title = "",
                            thumbnailUrl = dto.thumbnailUrl,
                            videoUrl = "https://www.youtube.com/watch?v=$id",
                            videoId = id,
                            publishedAt = "",
                            similarity = 0f
                        )
                    }
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getPopularVideos() call failed", e); emptyList()
        }

        val monday00 = mondayStartOfThisWeekMillis()

        HomeSnapshot(
            weekStart = monday00,
            hashtagsTop10 = hashtags.take(10),
            risingTop10 = rising.take(10),
            popularVideos = videos
        )
    }

    private fun mondayStartOfThisWeekMillis(): Long {
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun mapHashtags(res: WeeklyTrendRes): List<HashtagRank> =
        res.best_hashtags.orEmpty().mapIndexed { idx, it ->
            val tag = normalizeTag(it.hashtag)
            HashtagRank(
                tag = tag,
                count = it.total_posts ?: 0,
                rank = idx + 1
            )
        }

    private fun mapRising(res: WeeklyTrendRes): List<RisingKeyword> {
        val total = res.rising_hashtags.orEmpty().sumOf { it.this_week ?: 0 }.coerceAtLeast(1)
        return res.rising_hashtags.orEmpty().mapIndexed { idx, it ->
            val tag = normalizeTag(raw = it.hashtag)
            val count = it.this_week ?: 0
            val pct = ((count.toFloat() / total.toFloat()) * 100f).roundToInt()
            RisingKeyword(
                keyword    = tag,
                count      = count,
                rank       = idx + 1,
                growthRate = (it.growth_rate ?: 0.0).toFloat(),
                percent    = pct
            )
        }
    }

    private fun normalizeTag(raw: String?): String {
        val s = raw.orEmpty().trim()
        return if (s.startsWith("#")) s else "#$s"
    }

    /** 썸네일 URL에서 유튜브 ID 추출 */
    private fun extractYoutubeIdFromThumb(url: String): String? {
        val marker = "/vi/"
        val i = url.indexOf(marker)
        if (i < 0) return null
        val start = i + marker.length
        val end = url.indexOf('/', start)
        return if (end > start) url.substring(start, end) else null
    }

    companion object { private const val TAG = "RealHomeRepository" }
}