package com.h.trendie.network

import com.h.trendie.data.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 로컬 서버
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // 업로드 바디까지 찍히면 로그가 매우 길어질 수 있음. 필요시 BASIC/HEADERS 로 낮추세요.
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)   // 307/308 리다이렉트 대응
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)                    // 반드시 슬래시로 끝남
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // 있으면 유지, 없으면 삭제해도 됨
    val youtubeApi: YoutubeApi by lazy {
        retrofit.create(YoutubeApi::class.java)
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}