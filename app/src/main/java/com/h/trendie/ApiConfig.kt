package com.h.trendie

object ApiConfig {
    const val BASE_URL = "https://skyewha-trendie.kr/"

    const val PREFS_USER = "user_secure_prefs"
    const val PREFS_APP  = "trendie_prefs"
    const val PREFS_FLOW = "login_flow_prefs"

    const val KEY_ACCESS   = "accessToken"
    const val KEY_REFRESH  = "refreshToken"
    const val KEY_PROVIDER = "provider"
    const val KEY_EMAIL    = "email"
    const val KEY_NICKNAME = "nickname"
    const val KEY_USER_ID  = "user_id"

    const val KEY_SELECTED_PREFERENCES = "selected_preferences"

    const val EP_SOCIAL_LOGIN = "/api/v1/auth/social/login"
    const val EP_SIGNUP       = "/api/v1/auth/signup"
    const val EP_EXCHANGE     = "/api/v1/auth/token/exchange"
}
