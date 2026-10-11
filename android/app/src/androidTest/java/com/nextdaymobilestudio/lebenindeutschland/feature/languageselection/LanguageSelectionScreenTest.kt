package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionRoute
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resolvedLanguageIsSelectedAndContinueIsEnabled() {
        setRoute(
            viewModel = LanguageSelectionViewModel(
                initialLanguage = SupportedLanguage.GERMAN,
            ),
        )

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("de"))
            .assertIsSelected()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton,
                ),
            )
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.CONTINUE)
            .assertIsEnabled()
    }

    @Test
    fun clickingLanguageUpdatesSelectedState() {
        val state = mutableStateOf(
            LanguageSelectionUiState(
                selectedLanguage = SupportedLanguage.GERMAN,
            ),
        )

        composeRule.setContent {
            LebenInDeutschlandTheme {
                LanguageSelectionScreen(
                    uiState = state.value,
                    onSearchQueryChanged = {},
                    onLanguageSelected = { language ->
                        state.value = state.value.copy(selectedLanguage = language)
                    },
                    onContinue = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("tr"))
            .performClick()

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("tr"))
            .assertIsSelected()
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("de"))
            .assertIsNotSelected()
    }

    @Test
    fun searchFiltersByAccentInsensitiveNativeName() {
        setRoute(
            viewModel = LanguageSelectionViewModel(
                initialLanguage = SupportedLanguage.GERMAN,
            ),
        )

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.SEARCH)
            .performTextReplacement("Turkce")

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("tr"))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("de"))
            .assertDoesNotExist()
    }

    @Test
    fun unmatchedSearchDisplaysEmptyState() {
        setRoute(
            viewModel = LanguageSelectionViewModel(
                initialLanguage = SupportedLanguage.GERMAN,
            ),
        )

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.SEARCH)
            .performTextReplacement("not-a-supported-language")

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.EMPTY_STATE)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun continueReturnsSelectedLanguage() {
        var selectedLanguage: SupportedLanguage? = null
        setRoute(
            viewModel = LanguageSelectionViewModel(
                initialLanguage = SupportedLanguage.TURKISH,
            ),
            onContinue = { language -> selectedLanguage = language },
        )

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.CONTINUE)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(SupportedLanguage.TURKISH, selectedLanguage)
        }
    }

    @Test
    fun missingSelectionDisablesContinue() {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                LanguageSelectionScreen(
                    uiState = LanguageSelectionUiState(selectedLanguage = null),
                    onSearchQueryChanged = {},
                    onLanguageSelected = {},
                    onContinue = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.CONTINUE)
            .assertIsNotEnabled()
    }

    @Test
    fun largeFontScaleKeepsCriticalControlsVisible() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 1.3f,
                ),
            ) {
                LebenInDeutschlandTheme {
                    LanguageSelectionScreen(
                        uiState = LanguageSelectionUiState(
                            selectedLanguage = SupportedLanguage.GERMAN,
                        ),
                        onSearchQueryChanged = {},
                        onLanguageSelected = {},
                        onContinue = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.SCREEN)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("de"))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.CONTINUE)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    private fun setRoute(
        viewModel: LanguageSelectionViewModel,
        onContinue: (SupportedLanguage) -> Unit = {},
    ) {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                LanguageSelectionRoute(
                    onContinue = onContinue,
                    viewModel = viewModel,
                )
            }
        }
    }
}
