package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSynchronizer
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageChangeViewModelTest {
    @Test
    fun `initial state is idle`() {
        assertEquals(LanguageChangeUiState.Idle, viewModel().uiState.value)
    }

    @Test
    fun `confirming the current language is ready without preparing a package`() {
        val viewModel = viewModel(currentLanguage = SupportedLanguage.TURKISH)

        viewModel.onLanguageConfirmed(SupportedLanguage.TURKISH)

        assertEquals(
            LanguageChangeUiState.Ready(SupportedLanguage.TURKISH),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `cancel returns to idle`() {
        val viewModel = viewModel(currentLanguage = SupportedLanguage.TURKISH)
        viewModel.onLanguageConfirmed(SupportedLanguage.TURKISH)

        viewModel.cancel()

        assertEquals(LanguageChangeUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `retry without a failed change does nothing`() {
        val viewModel = viewModel()

        viewModel.retry()

        assertEquals(LanguageChangeUiState.Idle, viewModel.uiState.value)
    }

    private fun viewModel(
        currentLanguage: SupportedLanguage = SupportedLanguage.GERMAN,
    ) = LanguageChangeViewModel(
        currentLanguage = currentLanguage,
        coordinator = LanguageChangeCoordinator(
            localContentRepository = FakeLanguagePackRepository(),
            contentSynchronizerProvider = { ContentSynchronizer { error("must not be called") } },
        ),
    )
}
