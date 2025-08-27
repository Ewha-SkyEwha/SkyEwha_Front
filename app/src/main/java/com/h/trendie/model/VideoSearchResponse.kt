package com.h.trendie.model

import com.squareup.moshi.Json

data class VideoSearchResponse(
    @Json(name = "results") val results: List<VideoItem>
)