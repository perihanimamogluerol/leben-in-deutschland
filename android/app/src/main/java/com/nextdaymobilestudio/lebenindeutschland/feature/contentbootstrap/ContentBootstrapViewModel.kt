package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContentBootstrapViewModel(
    private val selectedLanguageCode: String,
    private val coordinator: ContentBootstrapCoordinator,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ContentBootstrapUiState>(
        ContentBootstrapUiState.Loading,
    )
    val uiState: StateFlow<ContentBootstrapUiState> = _uiState.asStateFlow()

    private var bootstrapJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        bootstrapJob?.cancel()
        bootstrapJob = viewModelScope.launch {
            coordinator.bootstrap(selectedLanguageCode).collect { state ->
                _uiState.value = state
            }
        }
    }

    companion object {
        fun factory(
            selectedLanguageCode: String,
            coordinator: ContentBootstrapCoordinator,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ContentBootstrapViewModel(
                    selectedLanguageCode = selectedLanguageCode,
                    coordinator = coordinator,
                ) as T
        }
    }
}
