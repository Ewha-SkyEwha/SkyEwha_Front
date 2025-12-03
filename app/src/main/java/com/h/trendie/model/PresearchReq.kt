package com.h.trendie.model

data class PresearchReq(
    val feedback_id: Int? = null,
    val keywords: List<String> = emptyList()
)