package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LanguageChangeViewModel(
    private val currentLanguage: SupportedLanguage,
    private val coordinator: LanguageChangeCoordinator,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LanguageChangeUiState>(LanguageChangeUiState.Idle)
    val uiState: StateFlow<LanguageChangeUiState> = _uiState.asStateFlow()

    private var prepareJob: Job? = null

    fun onLanguageConfirmed(language: SupportedLanguage) {
        if (language == currentLanguage) {
            _uiState.value = LanguageChangeUiState.Ready(language)
            return
        }
        prepare(language)
    }

    fun retry() {
        val failedState = _uiState.value as? LanguageChangeUiState.Failed ?: return
        prepare(failedState.language)
    }

    fun cancel() {
        prepareJob?.cancel()
        prepareJob = null
        _uiState.value = LanguageChangeUiState.Idle
    }

    private fun prepare(language: SupportedLanguage) {
        prepareJob?.cancel()
        _uiState.value = LanguageChangeUiState.Preparing(language)
        prepareJob = viewModelScope.launch {
            _uiState.value = when (coordinator.prepare(language.code)) {
                LanguageChangeResult.Ready -> LanguageChangeUiState.Ready(language)
                LanguageChangeResult.Failed -> LanguageChangeUiState.Failed(language)
            }
        }
    }

    companion object {
        fun factory(
            currentLanguage: SupportedLanguage,
            coordinator: LanguageChangeCoordinator,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LanguageChangeViewModel(
                    currentLanguage = currentLanguage,
                    coordinator = coordinator,
                ) as T
        }
    }
}
