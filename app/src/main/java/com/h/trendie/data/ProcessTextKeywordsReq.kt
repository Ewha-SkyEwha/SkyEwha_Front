package com.h.trendie.data

import com.squareup.moshi.Json

data class ProcessTextKeywordsReq(
    @Json(name = "input_text") val inputText: String,
    @Json(name = "text_title") val textTitle: String
)