package com.h.trendie.data.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/* ----------------------------------------
 * 공통(사용자/토큰/성공응답)
 * ---------------------------------------- */

// user_id / userId / id 모두 수용, nickname/email도 이명 수용
@JsonClass(generateAdapter = true)
data class UserInfo internal constructor(
    // id
    @Json(name = "user_id")  private val _userIdSnake: Long? = null,
    @Json(name = "userId")   private val _userIdCamel: Long? = null,
    @Json(name = "id")       private val _idPlain:     Long? = null,

    // nickname
    @Json(name = "nickname")        private val _nicknameCamel: String? = null,
    @Json(name = "user_nickname")   private val _nicknameSnake: String? = null,

    // email
    @Json(name = "email")       private val _emailCamel: String? = null,
    @Json(name = "user_email")  private val _emailSnake: String? = null,
) {
    val userId: Long?    get() = _userIdCamel ?: _userIdSnake ?: _idPlain
    val nickname: String? get() = _nicknameCamel ?: _nicknameSnake
    val email: String?     get() = _emailCamel ?: _emailSnake
}

@JsonClass(generateAdapter = true)
data class SimpleSuccessRes(
    val success: Boolean,
    val message: String? = null,
    @Json(name = "kakao_logout_success")  val kakaoLogoutSuccess: Boolean? = null,
    @Json(name = "kakao_unlink_success")  val kakaoUnlinkSuccess: Boolean? = null,
    @Json(name = "google_logout_success") val googleLogoutSuccess: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class TokenSaveReq(
    @Json(name = "user_id")      val userId: String,
    @Json(name = "access_token") val accessToken: String
)

/* 로그인 URL */
@JsonClass(generateAdapter = true)
data class KakaoLoginUrlRes(@Json(name = "login_url") val loginUrl: String)
@JsonClass(generateAdapter = true)
data class GoogleLoginUrlRes(@Json(name = "login_url") val loginUrl: String)

/* ----------------------------------------
 * 로그인 결과
 * ---------------------------------------- */
@JsonClass(generateAdapter = true)
data class OAuthLoginRes internal constructor(

    @Json(name = "isNewUser")  private val _isNewCamel: Boolean? = null,
    @Json(name = "is_new_user")private val _isNewSnake: Boolean? = null,

    // 기존 회원 토큰
    @Json(name = "accessToken")  private val _accessCamel: String? = null,
    @Json(name = "access_token") private val _accessSnake: String? = null,

    @Json(name = "refreshToken")  private val _refreshCamel: String? = null,
    @Json(name = "refresh_token") private val _refreshSnake: String? = null,

    val user: UserInfo? = null,

    // 신규 회원 임시 토큰
    @Json(name = "tempToken")  private val _tempCamel: String? = null,
    @Json(name = "temp_token") private val _tempSnake: String? = null,

    // 프로필
    val email: String? = null,
    val name: String? = null,

    // 소셜별 액세스 토큰
    @Json(name = "kakao_access_token")  private val _kakaoSnake: String? = null,
    @Json(name = "kakaoAccessToken")    private val _kakaoCamel: String? = null,
    @Json(name = "google_access_token") private val _googleSnake: String? = null,
    @Json(name = "googleAccessToken")   private val _googleCamel: String? = null,
) {
    val isNewUser: Boolean  get() = _isNewCamel ?: _isNewSnake ?: false
    val accessToken: String?  get() = _accessCamel  ?: _accessSnake
    val refreshToken: String? get() = _refreshCamel ?: _refreshSnake
    val tempToken: String?    get() = _tempCamel    ?: _tempSnake
    val kakaoAccessToken: String?  get() = _kakaoCamel  ?: _kakaoSnake
    val googleAccessToken: String? get() = _googleCamel ?: _googleSnake
}

/* ----------------------------------------
 * 회원가입 요청
 * ---------------------------------------- */
@JsonClass(generateAdapter = true)
data class KakaoSignupReq(
    val nickname: String,
    @Json(name = "temp_token")   val tempToken: String,
    val name: String,
    val email: String,
    @Json(name = "access_token") val accessToken: String
)

@JsonClass(generateAdapter = true)
data class GoogleSignupReq(
    val nickname: String,
    @Json(name = "temp_token")   val tempToken: String,
    val name: String,
    val email: String,
    @Json(name = "access_token") val accessToken: String
)

/* ----------------------------------------
 * 회원가입 성공
 * ---------------------------------------- */
@JsonClass(generateAdapter = true)
data class AuthTokensRes internal constructor(
    @Json(name = "accessToken")  private val _accessCamel: String? = null,
    @Json(name = "access_token") private val _accessSnake: String? = null,
    @Json(name = "refreshToken") private val _refreshCamel: String? = null,
    @Json(name = "refresh_token")private val _refreshSnake: String? = null,
    val user: UserInfo
) {
    val accessToken: String get() = (_accessCamel ?: _accessSnake).orEmpty()
    val refreshToken: String get() = (_refreshCamel ?: _refreshSnake).orEmpty()
}

// 과거 이름 호환
typealias KakaoAuthTokensRes  = AuthTokensRes
typealias GoogleAuthTokensRes = AuthTokensRes
