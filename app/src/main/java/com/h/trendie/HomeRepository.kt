package com.h.trendie

import com.h.trendie.model.HashtagRank
import com.h.trendie.model.HomeSnapshot
import com.h.trendie.model.VideoItem

interface HomeRepository {
    suspend fun getSnapshot(): HomeSnapshot
}