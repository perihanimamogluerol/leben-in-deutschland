package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswer
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.ui.MockExamScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.ui.MockExamTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockExamScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupStartsOneExamThatUsesBothThresholds() {
        var started = false
        composeRule.setMockExamContent(
            uiState = MockExamUiState.Setup,
            onStartExam = { started = true },
        )

        composeRule.onNodeWithTag(MockExamTestTags.SETUP).assertIsDisplayed()
        composeRule.onNodeWithTag(MockExamTestTags.START)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(started)
        }
    }

    @Test
    fun runningExamShowsTimerAllowsNavigationAndConfirmsEarlySubmission() {
        var selectedAnswer: String? = null
        var movedNext = false
        var submitRequested = false
        val state = MockExamUiState.Ready(
            question = question(),
            currentIndex = 0,
            totalCount = 33,
            answeredCount = 0,
            selectedAnswerId = null,
            remainingSeconds = 3_600,
        )
        composeRule.setMockExamContent(
            uiState = state,
            onAnswerSelected = { answerId -> selectedAnswer = answerId },
            onNext = { movedNext = true },
            onRequestSubmit = { submitRequested = true },
        )

        composeRule.onNodeWithTag(MockExamTestTags.TIMER).assertIsDisplayed()
        composeRule.onNodeWithTag(MockExamTestTags.QUESTION).assertIsDisplayed()
        composeRule.onNodeWithTag(MockExamTestTags.answer("B")).performClick()
        composeRule.onNodeWithTag(MockExamTestTags.NEXT).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(MockExamTestTags.FINISH).performClick()

        composeRule.runOnIdle {
            assertEquals("B", selectedAnswer)
            assertTrue(movedNext)
            assertTrue(submitRequested)
        }
    }

    @Test
    fun confirmationExposesUnansweredQuestions() {
        val ready = MockExamUiState.Ready(
            question = question(),
            currentIndex = 8,
            totalCount = 33,
            answeredCount = 12,
            selectedAnswerId = "A",
            remainingSeconds = 2_400,
            showSubmitConfirmation = true,
        )
        composeRule.setMockExamContent(uiState = ready)
        composeRule.onNodeWithTag(MockExamTestTags.CONFIRM_DIALOG).assertIsDisplayed()
    }

    @Test
    fun resultShowsPassStatusAfterTimeout() {
        composeRule.setMockExamContent(
            uiState = MockExamUiState.Result(
                correctCount = 17,
                wrongCount = 10,
                unansweredCount = 6,
                totalCount = 33,
                submittedByTimeout = true,
            ),
        )
        composeRule.onNodeWithTag(MockExamTestTags.RESULT).assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setMockExamContent(
        uiState: MockExamUiState,
        onStartExam: () -> Unit = {},
        onAnswerSelected: (String) -> Unit = {},
        onNext: () -> Unit = {},
        onRequestSubmit: () -> Unit = {},
    ) {
        setContent {
            LebenInDeutschlandTheme {
                MockExamScreen(
                    uiState = uiState,
                    onBack = {},
                    onStartExam = onStartExam,
                    onAnswerSelected = onAnswerSelected,
                    onPrevious = {},
                    onNext = onNext,
                    onRequestSubmit = onRequestSubmit,
                    onCancelSubmit = {},
                    onConfirmSubmit = {},
                    onRetry = {},
                    onStartNewExam = {},
                    onFinished = {},
                )
            }
        }
    }
}

private fun question(): StudyQuestion = StudyQuestion(
    releaseId = "release-3",
    id = "general_001",
    officialNumber = 1,
    scope = "GENERAL",
    stateCode = null,
    category = null,
    correctAnswerId = "A",
    germanQuestionText = "Was ist richtig?",
    localizedQuestionText = "Hangisi doğrudur?",
    explanation = "Erklärung",
    languageCode = "tr",
    questionImage = null,
    answers = listOf(
        StudyAnswer("A", 0, "Antwort A", "Cevap A", null),
        StudyAnswer("B", 1, "Antwort B", "Cevap B", null),
        StudyAnswer("C", 2, "Antwort C", "Cevap C", null),
        StudyAnswer("D", 3, "Antwort D", "Cevap D", null),
    ),
    explanationStatus = "AI_GENERATED_NEEDS_REVIEW",
    reviewed = false,
)
