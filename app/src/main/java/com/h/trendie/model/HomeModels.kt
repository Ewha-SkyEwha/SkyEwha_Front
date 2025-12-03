package com.h.trendie.model

data class HashtagRank(
    val tag: String,
    val count: Int,
    val rank: Int
)

/** 급상승 키워드 */

data class RisingKeyword(
    val keyword: String,
    val count: Int,
    val rank: Int,
    val percent: Int? = null,
    val growthRate: Float? = null
)


/** 홈 탭 스냅샷 */
data class HomeSnapshot(
    val weekStart: Long,
    val hashtagsTop10: List<HashtagRank>,
    val risingTop10: List<RisingKeyword>,
    val popularVideos: List<VideoItem>
)
