package com.h.trendie

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NicknameBus {
    private val _flow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val flow = _flow.asSharedFlow()

    fun emit(name: String) {
        _flow.tryEmit(name)
    }
}