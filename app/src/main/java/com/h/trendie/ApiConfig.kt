package com.h.trendie

object ApiConfig {

    const val BASE_URL = "http://10.0.2.2:8000" //팀 서버로 교체

    const val PREFS_USER = "user_prefs"           // 토큰
    const val KEY_ACCESS = "accessToken"
    const val KEY_REFRESH = "refreshToken"
    const val KEY_PROVIDER = "provider"           // "kakao" | "google"
    const val KEY_EMAIL = "email"
    const val KEY_NICKNAME = "nickname"
    const val KEY_PREF_CHOICES = "preference_choices"
    const val KEY_INSTALL_DATE = "install_date"
    const val KEY_DARK_MODE = "dark_mode"
    const val PREFS_APP = "trendie_prefs"         // 다크모드 등

    const val PRESEARCH = "/api/v1/youtube/presearch"
}