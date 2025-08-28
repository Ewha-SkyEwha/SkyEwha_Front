package com.h.trendie

import com.h.trendie.model.HomeSnapshot

interface HomeRepository {
    suspend fun getSnapshot(): HomeSnapshot
}