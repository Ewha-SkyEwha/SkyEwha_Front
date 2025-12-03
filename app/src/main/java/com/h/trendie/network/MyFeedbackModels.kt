package com.h.trendie.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MyFeedbackItem(
    @Json(name = "source_title") val sourceTitle: String?,
    @Json(name = "titles") val titles: List<String>?,
    @Json(name = "hashtags") val hashtags: List<String>?,
    @Json(name = "similar_videos") val similarVideos: List<SimilarVideoDto>?
)

@JsonClass(generateAdapter = true)
data class SimilarVideoDto(
    @Json(name = "video_id") val videoId: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "video_url") val videoUrl: String?,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?,
    @Json(name = "published_at") val publishedAt: String?,
    @Json(name = "similarity") val similarity: Double?
)