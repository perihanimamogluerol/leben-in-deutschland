package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage

sealed interface LanguageChangeUiState {
    data object Idle : LanguageChangeUiState

    data class Preparing(val language: SupportedLanguage) : LanguageChangeUiState

    data class Failed(val language: SupportedLanguage) : LanguageChangeUiState

    data class Ready(val language: SupportedLanguage) : LanguageChangeUiState
}
