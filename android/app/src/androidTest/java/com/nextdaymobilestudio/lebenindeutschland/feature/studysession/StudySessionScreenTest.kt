package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswer
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.feature.studysession.ui.StudySessionScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.studysession.ui.StudySessionTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudySessionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun questionRequiresASelectionBeforeSubmission() {
        var selectedAnswerId: String? = null
        setScreen(
            state = readyState(),
            onAnswerSelected = { selectedAnswerId = it },
        )

        composeRule.onNodeWithTag(StudySessionTestTags.QUESTION).assertIsDisplayed()
        composeRule.onNodeWithText("Was ist richtig?").assertIsDisplayed()
        composeRule.onNodeWithText("Hangisi doğrudur?").assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.ACTION).assertIsNotEnabled()

        composeRule.onNodeWithTag(StudySessionTestTags.answer("B")).performClick()

        composeRule.runOnIdle { assertEquals("B", selectedAnswerId) }
    }

    @Test
    fun submittedAnswerShowsCorrectnessAndExplanation() {
        setScreen(
            state = readyState(
                selectedAnswerId = "A",
                isSubmitted = true,
            ),
        )

        composeRule.onNodeWithTag(StudySessionTestTags.EXPLANATION).assertIsDisplayed()
        composeRule.onNodeWithText("Das ist die Erklärung.").assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.ACTION).assertIsEnabled()
    }

    @Test
    fun errorCanRetry() {
        var retried = false
        setScreen(
            state = StudySessionUiState.Error,
            onRetry = { retried = true },
        )

        composeRule.onNodeWithTag(StudySessionTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.RETRY).performClick()
        composeRule.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun completedSessionCanReturnHome() {
        var finished = false
        setScreen(
            state = StudySessionUiState.Complete(
                questionCount = 2,
                correctCount = 1,
            ),
            onFinished = { finished = true },
        )

        composeRule.onNodeWithTag(StudySessionTestTags.COMPLETE).assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.BACK_HOME).performClick()
        composeRule.runOnIdle { assertTrue(finished) }
    }

    private fun setScreen(
        state: StudySessionUiState,
        onAnswerSelected: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onFinished: () -> Unit = {},
    ) {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                StudySessionScreen(
                    uiState = state,
                    onBack = {},
                    onAnswerSelected = onAnswerSelected,
                    onSubmitAnswer = {},
                    onNextQuestion = {},
                    onRetry = onRetry,
                    onFinished = onFinished,
                )
            }
        }
    }
}

private fun readyState(
    selectedAnswerId: String? = null,
    isSubmitted: Boolean = false,
): StudySessionUiState.Ready = StudySessionUiState.Ready(
    question = testQuestion(),
    currentIndex = 0,
    totalCount = 2,
    selectedAnswerId = selectedAnswerId,
    isSubmitted = isSubmitted,
)

private fun testQuestion(): StudyQuestion = StudyQuestion(
    releaseId = "release-3",
    id = "general_001",
    officialNumber = 1,
    scope = "GENERAL",
    stateCode = null,
    category = null,
    correctAnswerId = "A",
    germanQuestionText = "Was ist richtig?",
    localizedQuestionText = "Hangisi doğrudur?",
    explanation = "Das ist die Erklärung.",
    languageCode = "tr",
    questionImage = null,
    answers = listOf(
        StudyAnswer("A", 0, "Antwort A", "Cevap A", null),
        StudyAnswer("B", 1, "Antwort B", "Cevap B", null),
    ),
    explanationStatus = "AI_GENERATED_NEEDS_REVIEW",
    reviewed = false,
)
