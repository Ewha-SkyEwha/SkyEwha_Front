package com.h.trendie

import com.h.trendie.network.MyFeedbackItem
import com.h.trendie.network.SimilarVideoDto
import kotlin.math.abs

data class HistoryItem(
    val id: Long,
    val title: String,
    val date: String,
    val feedbackId: Long? = null,
    val titles: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val thumbs: List<String> = emptyList()
)

fun MyFeedbackItem.toHistoryItem(): HistoryItem {
    val source = sourceTitle?.trim().takeUnless { it.isNullOrBlank() }
    val recTitles = (titles ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() }

    val displayTitle = source ?: recTitles.firstOrNull() ?: "(제목 없음)"

    val displayThumbs = (similarVideos ?: emptyList())
        .mapNotNull { it.toDisplayItem() }
        .distinct()

    return HistoryItem(
        id = abs(displayTitle.hashCode().toLong()),
        title = displayTitle,
        date = "",
        feedbackId = null,
        titles = recTitles,
        hashtags = (hashtags ?: emptyList())
            .map { it.trim() }.filter { it.isNotBlank() }
            .distinct().take(10),
        thumbs = displayThumbs
    )
}

/** similar_videos → 썸네일/URL */
private fun SimilarVideoDto.toDisplayItem(): String? {
    val v = (videoUrl ?: "").trim()
    val t = (thumbnailUrl ?: "").trim()
    return when {
        v.contains("youtu", ignoreCase = true) -> v
        t.isNotBlank() -> t
        else -> null
    }
}