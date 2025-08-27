// MockHomeRepository.kt
package com.h.trendie

import com.h.trendie.model.HashtagRank
import com.h.trendie.model.HomeSnapshot
import com.h.trendie.model.RisingKeyword
import com.h.trendie.model.VideoItem

/** 홈 탭 더미 데이터 */
class MockHomeRepository : HomeRepository {   // ← Context 인자 제거

    override suspend fun getSnapshot(): HomeSnapshot {
        val tags = listOf(
            HashtagRank("#여름휴가", 123, 1),
            HashtagRank("#제주도",   98,  2),
            HashtagRank("#서핑",     75,  3),
            HashtagRank("#물놀이",   61,  4),
            HashtagRank("#캠핑",     58,  5),
            HashtagRank("#바다",     51,  6),
            HashtagRank("#계곡",     47,  7),
            HashtagRank("#풀빌라",   44,  8),
            HashtagRank("#스노클링", 41,  9),
            HashtagRank("#한여름",   39, 10),
        )

        val rising = listOf(
            RisingKeyword("키워드1", 1, 4294, 60f, +2),
            RisingKeyword("키워드2", 2, 2084, 34f, +1),
            RisingKeyword("키워드3", 3,  566, 20f,  0),
            RisingKeyword("키워드4", 4, 1955, 28f, +2),
            RisingKeyword("키워드5", 5,  806, 76f, +3),
            RisingKeyword("키워드6", 6, 3096, 56f, +4),
            RisingKeyword("키워드7", 7, 1652, 21f, -1),
            RisingKeyword("키워드8", 8, 2457, 53f, +1),
        )

        val videos = listOf(
            VideoItem(
                title = "제주 서핑 브이로그",
                thumbnailUrl = "https://picsum.photos/seed/v1/600/338",
                videoUrl = "https://www.youtube.com/watch?v=dummy1",
                publishedAt = "2025-07-17T07:01:04",
                similarity = 0.83
            ),
            VideoItem(
                title = "속초 해수욕장 꿀팁",
                thumbnailUrl = "https://picsum.photos/seed/v2/600/338",
                videoUrl = "https://www.youtube.com/watch?v=dummy2",
                publishedAt = "2025-07-18T09:15:00",
                similarity = 0.77
            ),
            VideoItem(
                title = "보홀 스노클링 스팟",
                thumbnailUrl = "https://picsum.photos/seed/v3/600/338",
                videoUrl = "https://www.youtube.com/watch?v=dummy3",
                publishedAt = "2025-07-18T12:30:00",
                similarity = 0.74
            )
        )

        val monday00 = java.time.LocalDate.now()
            .with(java.time.DayOfWeek.MONDAY)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        return HomeSnapshot(
            weekStart = monday00,
            hashtagsTop10 = tags,
            risingTop10 = rising,
            popularVideos = videos
        )
    }
}