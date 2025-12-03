// app/src/main/java/com/h/trendie/network/FeedbackResponse.kt
package com.h.trendie.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.h.trendie.network.SimilarVideo

@JsonClass(generateAdapter = true)
data class FeedbackResponse(

    @Json(name = "feedback_id") val feedbackId: Long? = null,
    @Json(name = "score")       val score: Double? = null,
    @Json(name = "message")     val message: String? = null,
    @Json(name = "keywords")    val keywords: List<String>? = null,

    // 기존/호환 필드
    @Json(name = "titles")               val titles: List<String>? = null,
    @Json(name = "recommended_titles")   val titlesAlt: List<String>? = null,
    @Json(name = "hashtags")             val hashtags: List<String>? = null,
    @Json(name = "similar_videos")       val similarVideos: List<SimilarVideo>? = null
) {
    val titlesSafe: List<String> get() = titles ?: titlesAlt ?: emptyList()
    val hashtagsSafe: List<String> get() = hashtags ?: emptyList()
    val similarThumbs: List<String> get() =
        similarVideos?.mapNotNull { it.thumbnailUrl } ?: emptyList()
    val similarLinks: List<String> get() =
        similarVideos?.mapNotNull { it.videoUrl } ?: emptyList()
}