package com.h.trendie

// title을 선택값(기본값)으로 만들어서 어디서든 value만 넣어도 생성되게 함
data class PreferenceItem(
    val title: String = "내가 선호하는 여행",
    val value: String
)
