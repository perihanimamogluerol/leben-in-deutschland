package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui.LanguageChangeDialog
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui.SettingsScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui.SettingsTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsShowCurrentSelectionAndOpenChangeFlows() {
        var languageChanges = 0
        var stateChanges = 0
        var backs = 0

        composeRule.setContent {
            LebenInDeutschlandTheme {
                SettingsScreen(
                    languageName = "Türkçe",
                    stateName = "Hessen",
                    onBack = { backs += 1 },
                    onChangeLanguage = { languageChanges += 1 },
                    onChangeState = { stateChanges += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Türkçe").assertIsDisplayed()
        composeRule.onNodeWithText("Hessen").assertIsDisplayed()

        composeRule.onNodeWithTag(SettingsTestTags.CHANGE_LANGUAGE).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.CHANGE_STATE).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.BACK).performClick()

        assertEquals(1, languageChanges)
        assertEquals(1, stateChanges)
        assertEquals(1, backs)
    }

    @Test
    fun preparingDialogOffersCancelOnly() {
        var cancelled = 0

        composeRule.setContent {
            LebenInDeutschlandTheme {
                LanguageChangeDialog(
                    uiState = LanguageChangeUiState.Preparing(SupportedLanguage.TURKISH),
                    onRetry = {},
                    onCancel = { cancelled += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.PREPARING_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.RETRY).assertDoesNotExist()
        composeRule.onNodeWithTag(SettingsTestTags.CANCEL).performClick()

        assertEquals(1, cancelled)
    }

    @Test
    fun failedDialogOffersRetryAndCancel() {
        var retries = 0
        var cancelled = 0

        composeRule.setContent {
            LebenInDeutschlandTheme {
                LanguageChangeDialog(
                    uiState = LanguageChangeUiState.Failed(SupportedLanguage.TURKISH),
                    onRetry = { retries += 1 },
                    onCancel = { cancelled += 1 },
                )
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.FAILED_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.RETRY).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.CANCEL).performClick()

        assertEquals(1, retries)
        assertEquals(1, cancelled)
    }
}
