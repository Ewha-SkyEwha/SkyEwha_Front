package com.h.trendie

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

interface HomeRepository {
    suspend fun loadHomeSnapshot(force: Boolean = false): HomeSnapshot
}

class DefaultHomeRepository(private val ctx: Context) : HomeRepository {

    override suspend fun loadHomeSnapshot(force: Boolean): HomeSnapshot {
        val prefs = ctx.getSharedPreferences("home", Context.MODE_PRIVATE)
        val last = prefs.getLong("weekStart", 0L)
        val nowWeek = weekStartMillis()

        // v2 캐시 읽기
        val cachedJson = prefs.getString("snapshot_v2", null)
        val useCache = !force && last == nowWeek && cachedJson != null
        if (useCache) return fromJson(cachedJson)

        // ** 더미 데이터 생성 (rank/count/growthRate)
        val hashtags = listOf(
            "여름휴가","힐링여행","해외여행","워터파크","beach",
            "바닷가","캠핑","island","mountain","roadtrip"
        ).mapIndexed { i, t -> HashtagRank(t, (80..300).random(), i + 1) }

        val rising = (1..10).map { r ->
            val mentions = (500..5000).random()
            val growth   = (5..80).random().toFloat()
            RisingKeyword(
                keyword = "키워드$r",
                rank = r,              // 순위
                count = mentions,      // 언급량
                growthRate = growth,   // 퍼센트
                rankChange = 0
            )
        }

        val videos = (1..12).map {
            VideoItem("$it", "인기 영상 $it", "https://picsum.photos/seed/$it/300/200")
        }

        val snap = HomeSnapshot(nowWeek, hashtags, rising, videos)

        // v2 캐시로 저장
        prefs.edit()
            .putLong("weekStart", nowWeek)
            .putString("snapshot_v2", toJson(snap))
            .apply()

        return snap
    }

    // 이번 주 millis
    private fun weekStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return cal.timeInMillis
    }

    // ---------- JSON (v2) ----------
    private fun toJson(s: HomeSnapshot): String = JSONObject().apply {
        put("weekStart", s.weekStart)

        put("hashtags", JSONArray().apply {
            s.hashtagsTop10.forEach {
                put(JSONObject().apply {
                    put("tag", it.tag)
                    put("count", it.count)
                    put("rank", it.rank)
                })
            }
        })

        put("rising", JSONArray().apply {
            s.risingTop10.forEach {
                put(JSONObject().apply {
                    put("keyword", it.keyword)
                    put("rank", it.rank)           // 저장
                    put("count", it.count)         // 저장
                    put("growthRate", it.growthRate)
                    put("rankChange", it.rankChange)
                })
            }
        })

        put("videos", JSONArray().apply {
            s.popularVideos.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("thumbnailUrl", it.thumbnailUrl)
                })
            }
        })
    }.toString()

    private fun fromJson(j: String): HomeSnapshot {
        val o = JSONObject(j)

        val hashtags = o.getJSONArray("hashtags").let { arr ->
            (0 until arr.length()).map { i ->
                val x = arr.getJSONObject(i)
                HashtagRank(
                    tag = x.getString("tag"),
                    count = x.optInt("count", 0),
                    rank = x.optInt("rank", i + 1)
                )
            }
        }

        val rising = o.getJSONArray("rising").let { arr ->
            (0 until arr.length()).map { i ->
                val x = arr.getJSONObject(i)
                RisingKeyword(
                    keyword    = x.getString("keyword"),
                    rank       = x.optInt("rank", i + 1),          // 읽기
                    count      = x.optInt("count", 0),             // 읽기
                    growthRate = x.optDouble("growthRate", 0.0).toFloat(),
                    rankChange = x.optInt("rankChange", 0)
                )
            }
        }

        val videos = o.getJSONArray("videos").let { arr ->
            (0 until arr.length()).map { i ->
                val x = arr.getJSONObject(i)
                VideoItem(
                    id = x.getString("id"),
                    title = x.getString("title"),
                    thumbnailUrl = x.getString("thumbnailUrl")
                )
            }
        }

        return HomeSnapshot(
            weekStart = o.getLong("weekStart"),
            hashtagsTop10 = hashtags,
            risingTop10 = rising,
            popularVideos = videos
        )
    }
}