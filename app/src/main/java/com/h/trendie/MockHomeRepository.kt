package com.h.trendie

import android.content.Context

class MockHomeRepository(private val ctx: Context) : HomeRepository {
    override suspend fun loadHomeSnapshot(force: Boolean): HomeSnapshot {
        val hashtags = listOf("여름휴가","힐링여행","해외여행","워터파크","beach","바닷가","캠핑","island","mountain","roadtrip")
            .mapIndexed { idx, tag ->
                // 80~300 범위 값 보장
                HashtagRank(tag, count = (80..300).random(), rank = idx + 1)
            }

        val rising = (1..10).map { r ->
            val mentions = (500..5000).random()               // 언급량
            val growth = (5..80).random().toFloat()           // 상승 퍼센트
            RisingKeyword(
                keyword = "키워드$r",
                rank = r,
                count = mentions,
                growthRate = growth
            )
        }
        val videos = (1..12).map {
            VideoItem("$it","인기 영상 $it","https://picsum.photos/seed/$it/300/200")
        }
        return HomeSnapshot(System.currentTimeMillis(), hashtags, rising, videos)
    }
}