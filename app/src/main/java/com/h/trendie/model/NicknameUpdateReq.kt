package com.h.trendie.network

import com.squareup.moshi.Json

data class NicknameUpdateReq(
    @Json(name = "user_nickname") val userNickname: String
)
