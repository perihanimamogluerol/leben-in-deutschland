package com.nextdaymobilestudio.lebenindeutschland.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.feature.home.HomeProgressUiState
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.MockExamSnapshot
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.data.MockExamPreferencesRepository
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeRoute(
    languageName: String,
    stateName: String,
    selectedLanguageCode: String,
    selectedStateCode: String?,
    localContentRepository: LocalContentRepository,
    mockExamPreferencesRepository: MockExamPreferencesRepository,
    onStartStudy: () -> Unit,
    onReviewWrongAnswers: () -> Unit,
    onOpenMockExam: () -> Unit,
) {
    val progress by produceState(
        initialValue = HomeProgressUiState(),
        selectedLanguageCode,
        selectedStateCode,
        localContentRepository,
    ) {
        val release = localContentRepository.getActiveRelease() ?: return@produceState
        val eligibleIds = localContentRepository.getEligibleQuestionIds(
            releaseId = release.releaseId,
            languageCode = selectedLanguageCode,
            stateCode = selectedStateCode,
        )
        localContentRepository.observeStudyProgress(
            releaseId = release.releaseId,
            languageCode = selectedLanguageCode,
            stateCode = selectedStateCode,
        ).collectLatest { summary ->
            value = HomeProgressUiState(
                answeredCount = summary.answeredCount.coerceAtMost(eligibleIds.size),
                correctCount = summary.correctCount.coerceAtMost(eligibleIds.size),
                totalCount = eligibleIds.size,
            )
        }
    }
    val savedMockExam by produceState<MockExamSnapshot?>(
        initialValue = null,
        mockExamPreferencesRepository,
    ) {
        mockExamPreferencesRepository.exam.collectLatest { exam -> value = exam }
    }
    HomeScreen(
        languageName = languageName,
        stateName = stateName,
        studyProgress = progress,
        hasActiveMockExam = savedMockExam?.isComplete == false,
        hasCompletedMockExam = savedMockExam?.isComplete == true,
        onStartStudy = onStartStudy,
        onReviewWrongAnswers = onReviewWrongAnswers,
        onOpenMockExam = onOpenMockExam,
    )
}
