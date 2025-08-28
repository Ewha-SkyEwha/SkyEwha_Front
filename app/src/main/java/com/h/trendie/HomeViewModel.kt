package com.h.trendie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h.trendie.HomeRepository
import com.h.trendie.model.HomeSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: Throwable? = null
)

class HomeViewModel(
    private val repo: HomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UiState<HomeSnapshot>(loading = true))
    val state: StateFlow<UiState<HomeSnapshot>> = _state

    init {
        viewModelScope.launch {
            try {
                val snap = repo.getSnapshot()
                _state.value = UiState(data = snap)
            } catch (t: Throwable) {
                _state.value = UiState(error = t)
            }
        }
    }
}