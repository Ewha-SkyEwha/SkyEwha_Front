package com.h.trendie.network

import PopularVideosRes
import com.h.trendie.data.*
import com.h.trendie.data.auth.*
import com.h.trendie.model.*
import com.h.trendie.data.ProcessVideoKeywordsReq
import com.h.trendie.data.ProcessTextKeywordsReq
import UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ---------------------- Mypage ----------------------
    @PATCH("api/v1/mypage/users/{user_id}/nickname")
    suspend fun updateNickname(
        @Path("user_id") userId: Long, @Body body: NicknameUpdateReq
    ): Response<Unit>

    // ---------------------- Auth: Kakao ----------------------
    @GET("api/v1/auth/kakao/login_url/")
    suspend fun getKakaoLoginUrl(): Response<KakaoLoginUrlRes>

    @GET("api/v1/auth/login/kakao-callback")
    suspend fun kakaoCallback(@Query("code") code: String): Response<OAuthLoginRes>

    @POST("api/v1/auth/kakao/login")
    suspend fun kakaoLoginPost(@Body body: Map<String, String>): Response<OAuthLoginRes>

    @POST("api/v1/auth/kakao/signup")
    suspend fun kakaoSignup(@Body body: KakaoSignupReq): Response<KakaoAuthTokensRes>

    @POST("api/v1/auth/kakao/logout")
    suspend fun kakaoLogout(@Header("Authorization") bearer: String): Response<SimpleSuccessRes>

    @POST("api/v1/auth/kakao/unlink")
    suspend fun kakaoUnlink(): Response<SimpleSuccessRes>

    @POST("api/v1/auth/kakao/token")
    suspend fun kakaoTokenSave(@Body body: TokenSaveReq): Response<SimpleSuccessRes>

    // ---------------------- Auth: Google ----------------------
    @GET("api/v1/auth/google/login_url/")
    suspend fun getGoogleLoginUrl(): Response<GoogleLoginUrlRes>

    @GET("api/v1/auth/google/callback")
    suspend fun googleCallback(@Query("code") code: String): Response<OAuthLoginRes>

    @POST("api/v1/auth/google/login")
    suspend fun googleLoginPost(@Body body: Map<String, String>): Response<OAuthLoginRes>

    @POST("api/v1/auth/google/signup")
    suspend fun googleSignup(@Body body: GoogleSignupReq): Response<GoogleAuthTokensRes>

    @POST("api/v1/auth/google/logout")
    suspend fun googleLogout(@Header("Authorization") bearer: String): Response<SimpleSuccessRes>

    @POST("api/v1/auth/google/unlink")
    suspend fun googleUnlink(): Response<SimpleSuccessRes>

    @POST("api/v1/auth/google/token")
    suspend fun googleTokenSave(@Body body: TokenSaveReq): Response<SimpleSuccessRes>

    // ---------------------- YouTube ----------------------
    @POST("api/v1/youtube/presearch")
    suspend fun presearch(@Body body: PresearchReq): Response<PresearchRes>

    @GET("api/v1/youtube/popular")
    suspend fun getPopularVideos(): Response<PopularVideosRes>

    @POST("api/v1/youtube/crawl")
    suspend fun crawl(): Response<SimpleSuccessRes>

    @GET("api/v1/youtube/crawl_channels")
    suspend fun crawlChannels(): Response<SimpleSuccessRes>

    // ---------------------- Weekly Trend ----------------------
    @GET("api/v1/weekly_trend/hashtag")
    suspend fun getWeeklyTrend(): Response<WeeklyTrendRes>

    // ---------------------- Video ----------------------
    @Multipart
    @POST("api/v1/video/upload_video/")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part, @Part("video_title") title: RequestBody
    ): Response<UploadResponse>

    @POST("api/v1/keyword/process_video_keywords/")
    suspend fun processVideoKeywords(@Body body: ProcessVideoKeywordsReq): Response<ProcessVideoKeywordsRes>

    @POST("api/v1/keyword/process_text_keywords/")
    suspend fun processTextKeywords(@Body body: ProcessTextKeywordsReq): Response<ProcessTextKeywordsRes>

    // ---------------------- Title / Hashtag ----------------------
    @POST("api/v1/title/recommend")
    suspend fun recommendTitle(@Body body: RecommendTitleReq): Response<RecommendTitleRes>

    @POST("api/v1/hashtag/hashtags")
    suspend fun createHashtag(@Body body: CreateHashtagReq): Response<CreateHashtagRes>

    @GET("api/v1/recommend_hashtags/feedback/{feedback_id}")
    suspend fun recommendHashtagsByFeedbackId(@Path("feedback_id") feedbackId: Long): Response<RecommendHashtagsRes>

    // ---------------------- Feedback ----------------------
    @GET("api/v1/feedback/{feedback_id}")
    suspend fun getFeedback(@Path("feedback_id") feedbackId: Long): Response<FeedbackResponse>

    @GET("api/v1/feedback/my-feedbacks/")
    suspend fun getMyFeedbacks(): Response<List<MyFeedbackItem>>

    @POST("api/v1/bookmarks/videos/{video_id}/bookmark")
    suspend fun addBookmark(@Path("video_id") videoId: String): retrofit2.Response<Unit>

    @DELETE("api/v1/bookmarks/videos/{video_id}/bookmark")
    suspend fun unbookmark(@Path("video_id") videoId: String): retrofit2.Response<Unit>

    @GET("api/v1/bookmarks/videos/bookmarks/me")
    suspend fun getMyBookmarkedVideos(): List<BookmarkedVideoDto>

}

data class BookmarkedVideoDto(
    val video_id: String,
    val title: String,
    val video_url: String,
    val thumbnail_url: String,
    val channel_title: String,
    val published_at: String,
    val view_count: Long,
    val bookmarked_at: String
)

data class BookmarkAddReq(val video_id: String)
