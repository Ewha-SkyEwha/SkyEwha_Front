// app/src/main/java/com/h/trendie/HomeModels.kt
package com.h.trendie.model

// ✅ 여기엔 VideoItem 선언하지 마세요 (삭제!)

/** 해시태그 집계 */
data class HashtagRank(
    val tag: String,
    val count: Int,
    val rank: Int
)

/** 급상승 키워드 카드용 */
data class RisingKeyword(
    val keyword: String,
    val rank: Int,
    val count: Int,
    val growthRate: Float,
    val rankChange: Int = 0,
    val delta: Int? = null
)

/** 홈 탭 스냅샷 */
data class HomeSnapshot(
    val weekStart: Long,
    val hashtagsTop10: List<HashtagRank>,
    val risingTop10: List<RisingKeyword>,
    val popularVideos: List<VideoItem> // ← model/VideoItem.kt의 VideoItem
)