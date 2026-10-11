package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.MainActivity
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionTestTags
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui.StateSelectionTestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageSelectionLocaleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startFromGerman() {
        setApplicationLocale("de")
    }

    @After
    fun returnToSystemLocale() {
        setApplicationLocale("")
    }

    @Test
    fun continueAppliesLocaleAndNavigationSurvivesActivityRecreation() {
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("en"))
            .performClick()
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.CONTINUE)
            .performClick()

        waitForApplicationLocale("en")
        waitForText("Select federal state")
        composeRule.onNodeWithText("Select federal state").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        waitForApplicationLocale("en")
        waitForText("Select federal state")
        composeRule.onNodeWithText("Select federal state").assertIsDisplayed()

        composeRule
            .onNodeWithTag(StateSelectionTestTags.BACK)
            .performClick()
        waitForText("Select language")
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("en"))
            .assertIsSelected()
    }

    @Test
    fun arabicLocaleUsesRightToLeftLayout() {
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.SEARCH)
            .performTextReplacement("ar")
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.option("ar"))
            .performClick()
        composeRule
            .onNodeWithTag(LanguageSelectionTestTags.CONTINUE)
            .performClick()

        waitForApplicationLocale("ar")
        waitForText("اختر الولاية الألمانية")
        composeRule.onNodeWithText("اختر الولاية الألمانية").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(
                View.LAYOUT_DIRECTION_RTL,
                composeRule.activity.resources.configuration.layoutDirection,
            )
        }
    }

    private fun setApplicationLocale(languageTag: String) {
        composeRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag),
            )
        }
        waitForApplicationLocale(languageTag)
    }

    private fun waitForApplicationLocale(languageTag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            AppCompatDelegate.getApplicationLocales().toLanguageTags() == languageTag
        }
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText(text)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
