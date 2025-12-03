package com.h.trendie.data

import com.squareup.moshi.Json

data class ProcessVideoKeywordsRes(
    @Json(name = "message") val message: String,
    @Json(name = "feedback_id") val feedbackId: String
)
