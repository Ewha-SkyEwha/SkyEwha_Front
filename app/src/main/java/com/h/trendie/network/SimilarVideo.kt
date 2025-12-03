package com.h.trendie.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SimilarVideo(
    @Json(name = "video_url")     val videoUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)