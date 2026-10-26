package com.nextdaymobilestudio.lebenindeutschland.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.feature.home.ui.HomeScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.home.ui.HomeTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsInstalledContentAndStartsStudy() {
        var started = false

        composeRule.setContent {
            LebenInDeutschlandTheme {
                HomeScreen(
                    questionCount = 460,
                    languageName = "Türkçe",
                    stateName = "Hessen",
                    studyProgress = HomeProgressUiState(
                        answeredCount = 3,
                        correctCount = 2,
                        totalCount = 310,
                    ),
                    onStartStudy = { started = true },
                )
            }
        }

        composeRule.onNodeWithTag(HomeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.CONTENT_SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.STUDY_PROGRESS).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.LANGUAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.STATE).assertIsDisplayed()
        composeRule.onNodeWithText("Türkçe").assertIsDisplayed()
        composeRule.onNodeWithText("Hessen").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.START_STUDY).performClick()

        composeRule.runOnIdle { assertTrue(started) }
    }
}
