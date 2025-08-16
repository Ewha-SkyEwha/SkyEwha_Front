package com.h.trendie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: MockHomeRepository) : ViewModel() {
    data class UiState(val loading:Boolean=true, val data:HomeSnapshot?=null, val error:String?=null)
    private val _state = MutableStateFlow(UiState()); val state: StateFlow<UiState> = _state
    init { refreshIfNeeded(false) }
    fun refreshIfNeeded(force:Boolean) = viewModelScope.launch {
        _state.value = UiState(loading=true)
        runCatching { repo.loadHomeSnapshot(force) }
            .onSuccess { _state.value = UiState(loading=false, data=it) }
            .onFailure { _state.value = UiState(loading=false, error=it.message) }
    }
}