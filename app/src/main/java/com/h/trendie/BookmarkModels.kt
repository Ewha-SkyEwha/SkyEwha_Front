package com.h.trendie.bookmark

data class VideoItem(
    val id: String,
    val title: String,
    val channel: String,
    val durationText: String,
    val thumbUrl: String,
    val hashtags: List<String> = emptyList()
)
