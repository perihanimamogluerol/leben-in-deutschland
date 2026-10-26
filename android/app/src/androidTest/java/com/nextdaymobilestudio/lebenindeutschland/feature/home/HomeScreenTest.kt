package com.nextdaymobilestudio.lebenindeutschland.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
        var reviewedWrongAnswers = false
        var openedMockExam = false
        var openedExamHistory = false
        var openedSettings = false

        composeRule.setContent {
            LebenInDeutschlandTheme {
                HomeScreen(
                    languageName = "Türkçe",
                    stateName = "Hessen",
                    studyProgress = HomeProgressUiState(
                        answeredCount = 3,
                        correctCount = 2,
                        totalCount = 310,
                    ),
                    onStartStudy = { started = true },
                    onReviewWrongAnswers = { reviewedWrongAnswers = true },
                    onOpenMockExam = { openedMockExam = true },
                    onOpenExamHistory = { openedExamHistory = true },
                    onOpenSettings = { openedSettings = true },
                )
            }
        }

        composeRule.onNodeWithTag(HomeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.OPEN_SETTINGS).performClick()
        composeRule.onNodeWithTag(HomeTestTags.CONTENT_SUMMARY).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeTestTags.STUDY_PROGRESS).assertIsDisplayed()
        val contentList = composeRule.onNodeWithTag(HomeTestTags.CONTENT_LIST)
        contentList.performScrollToNode(hasTestTag(HomeTestTags.LANGUAGE))
        composeRule.onNodeWithTag(HomeTestTags.LANGUAGE).assertIsDisplayed()
        composeRule.onNodeWithText("Türkçe").assertIsDisplayed()
        contentList.performScrollToNode(hasTestTag(HomeTestTags.STATE))
        composeRule.onNodeWithTag(HomeTestTags.STATE).assertIsDisplayed()
        composeRule.onNodeWithText("Hessen").assertIsDisplayed()
        contentList.performScrollToNode(hasTestTag(HomeTestTags.REVIEW_WRONG_ANSWERS))
        composeRule.onNodeWithTag(HomeTestTags.REVIEW_WRONG_ANSWERS)
            .performClick()
        contentList.performScrollToNode(hasTestTag(HomeTestTags.OPEN_MOCK_EXAM))
        composeRule.onNodeWithTag(HomeTestTags.OPEN_MOCK_EXAM).performClick()
        contentList.performScrollToNode(hasTestTag(HomeTestTags.OPEN_EXAM_HISTORY))
        composeRule.onNodeWithTag(HomeTestTags.OPEN_EXAM_HISTORY).performClick()
        composeRule.onNodeWithTag(HomeTestTags.START_STUDY).performClick()

        composeRule.runOnIdle {
            assertTrue(started)
            assertTrue(reviewedWrongAnswers)
            assertTrue(openedMockExam)
            assertTrue(openedExamHistory)
            assertTrue(openedSettings)
        }
    }
}
