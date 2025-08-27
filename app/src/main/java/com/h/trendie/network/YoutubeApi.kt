package com.h.trendie.network

import com.h.trendie.model.PresearchReq
import com.h.trendie.model.PresearchRes
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface YoutubeApi {
    @POST("/api/v1/youtube/presearch")
    suspend fun presearch(@Body body: PresearchReq): Response<PresearchRes>
}