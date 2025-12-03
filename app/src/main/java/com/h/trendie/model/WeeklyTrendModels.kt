package com.h.trendie.model

data class TrendHashtagItem(
    val hashtag: String?,
    val total_posts: Int? = null,
    val this_week: Int? = null,
    val growth_rate: Float? = null
)

data class WeeklyTrendRes (
    val best_hashtags: List<TrendHashtagItem> = emptyList(),
    val rising_hashtags: List<TrendHashtagItem> = emptyList()
)