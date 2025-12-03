package com.h.trendie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class KeywordViewModel(
    private val state: SavedStateHandle
) : ViewModel() {

    private val _text = MutableStateFlow(state.get<String>(KEY) ?: "")
    val text: StateFlow<String> = _text

    fun setText(v: String) {
        _text.value = v
        state[KEY] = v
    }

    companion object {
        private const val KEY = "keyword_text_state"
    }
}
