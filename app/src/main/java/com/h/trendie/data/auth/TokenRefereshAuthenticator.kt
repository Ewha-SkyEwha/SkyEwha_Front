package com.h.trendie.network

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 401 발생 시 refresh → 원요청 1회 재시도
 */
class TokenRefreshAuthenticator(
    baseUrl: String,
    private val tokenProvider: TokenProviderAccessor
) : Authenticator {

    private val refreshApi: RefreshApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(RefreshApi::class.java)

    override fun authenticate(route: Route?, response: Response): Request? {
        // 무한루프 방지
        if (responseCount(response) >= 2) {
            Log.w("Auth", "Retry exceeded for ${response.request.url}")
            return null
        }

        val refresh = tokenProvider.getRefreshToken() ?: return null.also {
            Log.w("Auth", "No refresh token; cannot refresh")
        }

        return try {
            val r = refreshApi.refresh(RefreshReq(refresh)).execute()
            if (!r.isSuccessful) {
                Log.e("Auth", "Refresh failed: code=${r.code()} err=${r.errorBody()?.string()}")
                return null
            }
            val body = r.body()
            val newAccess = body?.accessToken
            val newRefresh = body?.refreshToken

            if (newAccess.isNullOrBlank()) {
                Log.e("Auth", "Refresh response missing access token")
                return null
            }

            tokenProvider.saveTokens(newAccess, newRefresh)
            Log.i("Auth", "Token refreshed; retrying original request")

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        } catch (e: Exception) {
            Log.e("Auth", "Refresh exception", e)
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) { count++; r = r.priorResponse }
        return count
    }
}

data class RefreshReq(val refresh: String)
data class RefreshRes(
    val accessToken: String?,
    val refreshToken: String?
)
interface RefreshApi {
    @POST("api/v1/auth/refresh")
    fun refresh(@Body req: RefreshReq): retrofit2.Call<RefreshRes>
}

interface TokenProviderAccessor {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(access: String?, refresh: String?)
}
