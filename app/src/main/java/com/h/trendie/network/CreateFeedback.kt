package com.h.trendie.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateFeedbackRes(
    @Json(name = "feedback_id") val feedbackId: Long,
    @Json(name = "video_title") val videoTitle: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateFeedbackReq(
    @Json(name = "video_title") val videoTitle: String
)