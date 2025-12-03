package com.h.trendie.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val repo: BookmarkRepository = BookmarkRepository()
) : ViewModel() {

    // --- videos ---
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos = _videos.asStateFlow()

    // --- tags ---
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** 영상 북마크 목록 새로고침 */
    fun refreshVideos() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _videos.value = repo.loadMyBookmarkedVideos()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun unbookmarkAt(position: Int) {
        viewModelScope.launch {
            val cur = _videos.value.toMutableList()
            if (position !in cur.indices) return@launch

            val removed = cur.removeAt(position)
            _videos.value = cur

            _loading.value = true
            _error.value = null
            try {
                repo.unbookmark(removed.id)
            } catch (e: Exception) {
                cur.add(position, removed)
                _videos.value = cur
                _error.value = e.message ?: "북마크 해제 실패"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun unbookmark(item: VideoItem) {
        val idx = _videos.value.indexOfFirst { it.id == item.id }
        if (idx >= 0) unbookmarkAt(idx)
    }
}