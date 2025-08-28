package com.h.trendie.data

import com.h.trendie.HomeRepository
import com.h.trendie.model.HomeSnapshot
import com.h.trendie.network.ApiClient

class RealHomeRepository(
    private val api: ApiService = ApiClient.apiService
) : HomeRepository {
    override suspend fun getSnapshot(): HomeSnapshot {
        // TODO: 실제 API 호출로 교체
        return HomeSnapshot(
            weekStart = 0L,
            hashtagsTop10 = emptyList(),
            risingTop10 = emptyList(),
            popularVideos = emptyList()
        )
    }
}