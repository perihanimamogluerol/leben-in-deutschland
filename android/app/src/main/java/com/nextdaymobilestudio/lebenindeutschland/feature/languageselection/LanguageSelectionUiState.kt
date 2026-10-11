package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage

data class LanguageSelectionUiState(
    val query: String = "",
    val selectedLanguage: SupportedLanguage? = null,
    val visibleLanguages: List<SupportedLanguage> = SupportedLanguage.entries,
) {
    val canContinue: Boolean
        get() = selectedLanguage != null
}
