package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamHistoryItem
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamQuestionResult
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamResultRecord
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswer
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ui.ExamHistoryDetailScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ui.ExamHistoryScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ui.ExamHistoryTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExamHistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun historyShowsAttemptAndOpensItsDetail() {
        var openedAttemptId: String? = null
        var wentBack = false
        val attempt = historyItem()

        composeRule.setContent {
            LebenInDeutschlandTheme {
                ExamHistoryScreen(
                    uiState = ExamHistoryUiState.Content(listOf(attempt)),
                    onBack = { wentBack = true },
                    onOpenAttempt = { openedAttemptId = it },
                )
            }
        }

        composeRule.onNodeWithTag(ExamHistoryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamHistoryTestTags.attempt(attempt.attemptId))
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(ExamHistoryTestTags.BACK).performClick()

        composeRule.runOnIdle {
            assertEquals(attempt.attemptId, openedAttemptId)
            assertTrue(wentBack)
        }
    }

    @Test
    fun detailShowsQuestionStatusAndAnswers() {
        val summary = historyItem().copy(
            correctCount = 1,
            wrongCount = 1,
            unansweredCount = 0,
            totalCount = 2,
        )
        val firstResult = MockExamQuestionResult("general_001", 0, "A", "A")
        val secondResult = MockExamQuestionResult("general_002", 1, "B", "A")
        val result = MockExamResultRecord(summary, listOf(firstResult, secondResult))
        val state = ExamHistoryDetailUiState.Content(
            result = result,
            questions = listOf(
                ExamHistoryDetailUiState.QuestionItem(firstResult, question("general_001")),
                ExamHistoryDetailUiState.QuestionItem(secondResult, question("general_002")),
            ),
        )

        composeRule.setContent {
            LebenInDeutschlandTheme {
                ExamHistoryDetailScreen(uiState = state, onBack = {})
            }
        }

        composeRule.onNodeWithTag(ExamHistoryTestTags.DETAIL_SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithTag(ExamHistoryTestTags.question("general_001"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ExamHistoryTestTags.question("general_002"))
            .assertExists()
    }

    @Test
    fun emptyHistoryShowsEmptyState() {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                ExamHistoryScreen(
                    uiState = ExamHistoryUiState.Empty,
                    onBack = {},
                    onOpenAttempt = {},
                )
            }
        }

        composeRule.onNodeWithTag(ExamHistoryTestTags.EMPTY).assertIsDisplayed()
    }
}

private fun historyItem(): MockExamHistoryItem = MockExamHistoryItem(
    attemptId = "attempt-1",
    releaseId = "release-1",
    languageCode = "de",
    stateCode = "HE",
    startedAtEpochMillis = 1_000L,
    submittedAtEpochMillis = 121_000L,
    deadlineEpochMillis = 3_601_000L,
    correctCount = 17,
    wrongCount = 16,
    unansweredCount = 0,
    totalCount = 33,
    submittedByTimeout = false,
)

private fun question(id: String): StudyQuestion = StudyQuestion(
    releaseId = "release-1",
    id = id,
    officialNumber = 1,
    scope = "GENERAL",
    stateCode = null,
    category = null,
    correctAnswerId = "A",
    germanQuestionText = "Testfrage",
    localizedQuestionText = "Testfrage",
    explanation = "Erklärung",
    languageCode = "de",
    questionImage = null,
    answers = listOf(
        StudyAnswer("A", 0, "Antwort A", "Antwort A", null),
        StudyAnswer("B", 1, "Antwort B", "Antwort B", null),
    ),
    explanationStatus = "SOURCE",
    reviewed = true,
)
