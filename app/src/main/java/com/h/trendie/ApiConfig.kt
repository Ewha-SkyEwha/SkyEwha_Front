package com.h.trendie

object ApiConfig {

    const val BASE_URL = "https://api.ourteam.com" //팀 서버로 교체

    const val PREFS_USER = "user_prefs"           // 토큰 등
    const val KEY_ACCESS = "accessToken"
    const val KEY_REFRESH = "refreshToken"
    const val KEY_PROVIDER = "provider"           // "kakao" | "google"
    const val KEY_EMAIL = "email"

    const val PREFS_APP = "trendie_prefs"         // 다크모드 등 (기존 유지)
}