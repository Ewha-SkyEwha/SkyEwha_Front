package com.h.trendie.data

import com.squareup.moshi.Json

data class ProcessTextKeywordsRes(
    val message: String,
    @Json(name = "feedback_id") val feedbackId: Long
)