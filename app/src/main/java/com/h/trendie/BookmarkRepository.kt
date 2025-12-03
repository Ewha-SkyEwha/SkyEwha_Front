package com.h.trendie.bookmark

import com.h.trendie.network.ApiClient
import com.h.trendie.network.ApiService
import com.h.trendie.network.BookmarkedVideoDto

class BookmarkRepository(
    private val api: ApiService = ApiClient.apiService
) {
    suspend fun loadMyBookmarkedVideos(): List<VideoItem> =
        api.getMyBookmarkedVideos().map { it.toVideoItem() }

    suspend fun unbookmark(videoId: String) {
        val res = api.unbookmark(videoId)
        if (!res.isSuccessful) throw IllegalStateException("북마크 해제 실패: ${res.code()}")
    }
}

private fun BookmarkedVideoDto.toVideoItem(): VideoItem =
    VideoItem(
        id = video_id,
        title = title,
        channel = channel_title,
        durationText = "",
        thumbUrl = thumbnail_url,
        hashtags = emptyList()
    )
