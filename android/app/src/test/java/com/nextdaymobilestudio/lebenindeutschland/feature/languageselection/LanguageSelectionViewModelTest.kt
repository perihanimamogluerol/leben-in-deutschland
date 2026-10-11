package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageSelectionViewModelTest {
    @Test
    fun `initial state selects resolved device language and can continue`() {
        val viewModel = LanguageSelectionViewModel(
            initialLanguage = SupportedLanguage.TURKISH,
        )

        val state = viewModel.uiState.value

        assertEquals(21, state.visibleLanguages.size)
        assertEquals(SupportedLanguage.TURKISH, state.selectedLanguage)
        assertTrue(state.canContinue)
    }

    @Test
    fun `selecting another language updates the selection`() {
        val viewModel = germanViewModel()

        viewModel.onLanguageSelected(SupportedLanguage.TURKISH)

        val state = viewModel.uiState.value
        assertEquals(SupportedLanguage.TURKISH, state.selectedLanguage)
        assertTrue(state.canContinue)
    }

    @Test
    fun `search matches the German language name`() {
        val viewModel = germanViewModel()

        viewModel.onSearchQueryChanged("Türkisch")

        assertEquals(
            listOf(SupportedLanguage.TURKISH),
            viewModel.uiState.value.visibleLanguages,
        )
    }

    @Test
    fun `search ignores accents in the native language name`() {
        val viewModel = germanViewModel()

        viewModel.onSearchQueryChanged("Turkce")

        assertEquals(
            listOf(SupportedLanguage.TURKISH),
            viewModel.uiState.value.visibleLanguages,
        )
    }

    @Test
    fun `search supports right to left scripts`() {
        val viewModel = germanViewModel()

        viewModel.onSearchQueryChanged("العربية")

        assertEquals(
            listOf(SupportedLanguage.ARABIC),
            viewModel.uiState.value.visibleLanguages,
        )
    }

    private fun germanViewModel(): LanguageSelectionViewModel =
        LanguageSelectionViewModel(initialLanguage = SupportedLanguage.GERMAN)
}
