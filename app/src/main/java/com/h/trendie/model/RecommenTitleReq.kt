package com.h.trendie.data

import com.squareup.moshi.Json

/** 요청 바디
 */
data class RecommendTitleReq(
    @Json(name = "feedback_id") val feedbackId: Long? = null,
    @Json(name = "text") val text: String? = null
)

/** 응답 바디
 * - titles: 추천된 제목 목록
 * - optional: message/status 로깅용
 */
data class RecommendTitleRes(
    @Json(name = "titles") val titles: List<String> = emptyList(),
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

/* =========================
   Hashtag: Create
========================= */

/** 요청 바디
 */
data class CreateHashtagReq(
    @Json(name = "feedback_id") val feedbackId: Long,
    @Json(name = "hashtags") val hashtags: List<String>? = null
)

/** 응답 바디
 */
data class CreateHashtagRes(
    @Json(name = "hashtags") val hashtags: List<String> = emptyList(),
    @Json(name = "message") val message: String? = null
)

/* =========================
   Hashtag: Recommend by feedbackId
========================= */

/** 응답 바디
 * - feedbackId 기준 추천 해시태그
 */
data class RecommendHashtagsRes(
    @Json(name = "feedback_id") val feedbackId: Long? = null,
    @Json(name = "hashtags") val hashtags: List<String> = emptyList(),
    @Json(name = "message") val message: String? = null
)
