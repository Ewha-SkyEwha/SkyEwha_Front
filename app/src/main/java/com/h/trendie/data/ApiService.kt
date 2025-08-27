package com.h.trendie.data

import com.h.trendie.model.VideoSearchResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Body

/** 업로드 응답 */
data class UploadResponse(
    val success: Boolean,
    val url: String? = null,
    val video_id: Int? = null,
    val video_title: String? = null,
    val upload_date: String? = null,
    val user_id: String? = null
)

/** 키워드 처리 요청/응답 */
data class ProcessKeywordsRequest(val video_id: Int)
data class ProcessKeywordsResponse(val message: String)

interface ApiService {

    /** ✅ 서버와 일치: POST /api/v1/video/upload_video (multipart) */
    @Multipart
    @POST("api/v1/video/upload_video")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part,      // part name: "file"
        @Part("title") title: RequestBody    // part name: "title"
    ): Response<UploadResponse>

    interface ApiService {
        @POST("/api/v1/videos/search_by_keywords")
        suspend fun searchVideos(@Body body: Map<String, List<String>>): Response<VideoSearchResponse>
    }

    /** 키워드 추출 */
    @POST("api/v1/keyword/process_keywords")
    suspend fun processKeywords(
        @Body body: ProcessKeywordsRequest
    ): Response<ProcessKeywordsResponse>

    /** 해시태그 추천 */
    @GET("api/v1/recommend_hashtags/{video_id}")
    suspend fun recommendHashtags(
        @Path("video_id") videoId: Int
    ): Response<List<String>>
}