package com.h.trendie.model

import com.squareup.moshi.Json

data class VideoSearchResponse(
    @Json(name = "video_id")     val videoId: String,
    @Json(name = "title")        val title: String,
    @Json(name = "video_url")    val videoUrl: String?,
    @Json(name = "thumbnail_url")val thumbnailUrl: String?,
    @Json(name = "published_at") val publishedAt: String,
    @Json(name = "similarity")   val similarity: Float
)