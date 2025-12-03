package com.h.trendie.model

// 텍스트 입력 요청
data class CreateTextFeedbackReq(
    val text: String,
    val language: String? = "ko" // 필요 없으면 null
)

// 서버가 생성 후 돌려주는 ID (문자/숫자 모두 흡수)
data class CreateTextFeedbackRes(
    // 서버 응답이 {"feedback_id": 123} 또는 {"feedback_id":"123"} 어느 쪽이든 수용하려고 문자열로 둠
    val feedback_id: String
)

// 이미 있는 조회 응답 DTO
data class FeedbackResponse(
    val titles: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val similarVideos: List<SimilarVideoDto> = emptyList()
)

data class SimilarVideoDto(
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null
)
