package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LanguageSelectionViewModel(
    initialLanguage: SupportedLanguage,
) : ViewModel() {
    private val allLanguages = SupportedLanguage.entries

    private val _uiState = MutableStateFlow(
        LanguageSelectionUiState(selectedLanguage = initialLanguage),
    )
    val uiState: StateFlow<LanguageSelectionUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                query = query,
                visibleLanguages = allLanguages.filterBy(query),
            )
        }
    }

    fun onLanguageSelected(language: SupportedLanguage) {
        _uiState.update { currentState ->
            currentState.copy(selectedLanguage = language)
        }
    }

    companion object {
        fun factory(initialLanguage: SupportedLanguage): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    LanguageSelectionViewModel(initialLanguage = initialLanguage)
                }
            }
    }
}

private fun List<SupportedLanguage>.filterBy(query: String): List<SupportedLanguage> {
    val normalizedQuery = query.normalizedForSearch()
    if (normalizedQuery.isBlank()) return this

    return filter { language ->
        language.nativeName.normalizedForSearch().contains(normalizedQuery) ||
            language.germanName.normalizedForSearch().contains(normalizedQuery) ||
            language.code.normalizedForSearch().contains(normalizedQuery)
    }
}

private fun String.normalizedForSearch(): String =
    Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .trim()

private val COMBINING_MARKS = Regex("\\p{M}+")
