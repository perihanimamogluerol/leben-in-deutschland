package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswer
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyMemoryKey
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
    fun submittedAnswerShowsCollapsedMemoryKeyAndExpandedExplanation() {
        setScreen(
            state = readyState(
                selectedAnswerId = "A",
                isSubmitted = true,
            ),
        )

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertDoesNotExist()
        composeRule.onNodeWithTag(StudySessionTestTags.EXPLANATION)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Das ist die Erklärung.").assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.ACTION).assertIsEnabled()
    }

    @Test
    fun memoryKeyStartsCollapsedAndCanOpenBeforeSubmission() {
        setScreen(state = readyState())

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertDoesNotExist()

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).performClick()

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertIsDisplayed()
    }

    @Test
    fun memoryKeyIsHiddenWhenContentHasNone() {
        setScreen(
            state = readyState(
                selectedAnswerId = "A",
                isSubmitted = true,
                question = testQuestion(memoryKey = null),
            ),
        )
        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).assertDoesNotExist()
        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertDoesNotExist()
    }

    @Test
    fun submittedWrongAnswerReviewShowsMemoryKey() {
        setScreen(
            state = readyState(selectedAnswerId = "B", isSubmitted = true),
            mode = StudySessionMode.WRONG_ANSWERS,
        )

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertDoesNotExist()

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).performClick()

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertIsDisplayed()
    }

    @Test
    fun longQuestionKeepsMemoryKeyAvailableBeforeTheQuestion() {
        val longAnswer = "Diese Antwort ist absichtlich sehr lang, damit die Liste mehr Platz benötigt."
        setScreen(
            state = readyState(
                question = testQuestion(
                    germanQuestionText = "Diese Frage ist absichtlich besonders lang und prüft die Position der Merkhilfe.",
                    localizedQuestionText = "Bu soru hatırlama ipucunun konumunu sınamak için özellikle uzundur.",
                    answers = listOf(
                        StudyAnswer("A", 0, "$longAnswer A", "$longAnswer A", null),
                        StudyAnswer("B", 1, "$longAnswer B", "$longAnswer B", null),
                        StudyAnswer("C", 2, "$longAnswer C", "$longAnswer C", null),
                        StudyAnswer("D", 3, "$longAnswer D", "$longAnswer D", null),
                    ),
                ),
            ),
        )

        val memoryKeyTop = composeRule
            .onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val questionTop = composeRule
            .onNodeWithTag(StudySessionTestTags.QUESTION)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(memoryKeyTop < questionTop)

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY_TOGGLE).performClick()

        composeRule.onNodeWithTag(StudySessionTestTags.MEMORY_KEY).assertIsDisplayed()
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

    @Test
    fun emptyWrongAnswersCanReturnHome() {
        var finished = false
        setScreen(
            state = StudySessionUiState.EmptyWrongAnswers,
            mode = StudySessionMode.WRONG_ANSWERS,
            onFinished = { finished = true },
        )

        composeRule.onNodeWithTag(StudySessionTestTags.EMPTY_WRONG_ANSWERS)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(StudySessionTestTags.BACK_HOME).performClick()
        composeRule.runOnIdle { assertTrue(finished) }
    }

    private fun setScreen(
        state: StudySessionUiState,
        onAnswerSelected: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onFinished: () -> Unit = {},
        mode: StudySessionMode = StudySessionMode.ALL_QUESTIONS,
    ) {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                StudySessionScreen(
                    uiState = state,
                    mode = mode,
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
    question: StudyQuestion = testQuestion(),
): StudySessionUiState.Ready = StudySessionUiState.Ready(
    question = question,
    currentIndex = 0,
    totalCount = 2,
    selectedAnswerId = selectedAnswerId,
    isSubmitted = isSubmitted,
)

private fun testQuestion(
    memoryKey: StudyMemoryKey? = StudyMemoryKey(
        questionCue = "Frage erkennen",
        answerCue = "Antwort erinnern",
        status = "AI_GENERATED_NEEDS_REVIEW",
    ),
    germanQuestionText: String = "Was ist richtig?",
    localizedQuestionText: String = "Hangisi doğrudur?",
    answers: List<StudyAnswer> = listOf(
        StudyAnswer("A", 0, "Antwort A", "Cevap A", null),
        StudyAnswer("B", 1, "Antwort B", "Cevap B", null),
    ),
): StudyQuestion = StudyQuestion(
    releaseId = "release-3",
    id = "general_001",
    officialNumber = 1,
    scope = "GENERAL",
    stateCode = null,
    category = null,
    correctAnswerId = "A",
    germanQuestionText = germanQuestionText,
    localizedQuestionText = localizedQuestionText,
    explanation = "Das ist die Erklärung.",
    languageCode = "tr",
    questionImage = null,
    answers = answers,
    explanationStatus = "AI_GENERATED_NEEDS_REVIEW",
    reviewed = false,
    memoryKey = memoryKey,
)
