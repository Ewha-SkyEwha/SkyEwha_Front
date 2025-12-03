package com.h.trendie.model

import com.squareup.moshi.Json

data class PresearchRes(
    @Json(name = "results")
    val results: List<VideoSearchResponse>
)