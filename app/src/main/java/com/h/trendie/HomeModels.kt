package com.h.trendie

data class HashtagRank(val tag: String, val count: Int, val rank: Int)
data class RisingKeyword(
    val keyword: String,
    val rank: Int,          // 이번주 순위
    val count: Int,         // 이번주 언급량
    val growthRate: Float,  // 전주 대비 %
    val rankChange: Int = 0 // 기존 필드 유지용 (UI 사용x)
)
data class VideoItem(val id: String, val title: String, val thumbnailUrl: String)
data class HomeSnapshot(
    val weekStart: Long,
    val hashtagsTop10: List<HashtagRank>,
    val risingTop10: List<RisingKeyword>,
    val popularVideos: List<VideoItem>
)