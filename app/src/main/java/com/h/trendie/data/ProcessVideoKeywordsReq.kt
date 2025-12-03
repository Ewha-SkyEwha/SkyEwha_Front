package com.h.trendie.data

import com.squareup.moshi.Json

data class ProcessVideoKeywordsReq(
    @Json(name = "video_id") val videoId: Long
)