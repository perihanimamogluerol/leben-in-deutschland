package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui.SettingsScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui.SettingsTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsShowsSelectionsAndInvokesActions() {
        var wentBack = false
        var openedLanguage = false
        var openedState = false

        composeRule.setContent {
            LebenInDeutschlandTheme {
                SettingsScreen(
                    languageName = "Türkçe",
                    stateName = "Hessen",
                    onBack = { wentBack = true },
                    onChangeLanguage = { openedLanguage = true },
                    onChangeState = { openedState = true },
                )
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Türkçe").assertIsDisplayed()
        composeRule.onNodeWithText("Hessen").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.PROGRESS_NOTE).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.STATE).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.BACK).performClick()

        composeRule.runOnIdle {
            assertTrue(wentBack)
            assertTrue(openedLanguage)
            assertTrue(openedState)
        }
    }
}
