package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ExamHistoryDetailUiState
import com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ExamHistoryViewModel

@Composable
fun ExamHistoryRoute(
    localContentRepository: LocalContentRepository,
    onBack: () -> Unit,
    onOpenAttempt: (String) -> Unit,
    viewModel: ExamHistoryViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<ExamHistoryViewModel>(
        factory = ExamHistoryViewModel.factory(localContentRepository),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    ExamHistoryScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenAttempt = onOpenAttempt,
    )
}

@Composable
fun ExamHistoryDetailRoute(
    attemptId: String,
    localContentRepository: LocalContentRepository,
    onBack: () -> Unit,
) {
    val uiState by produceState<ExamHistoryDetailUiState>(
        initialValue = ExamHistoryDetailUiState.Loading,
        attemptId,
        localContentRepository,
    ) {
        value = try {
            val result = localContentRepository.getMockExamResult(attemptId)
            if (result == null) {
                ExamHistoryDetailUiState.NotFound
            } else {
                val questions = result.questionResults.map { questionResult ->
                    ExamHistoryDetailUiState.QuestionItem(
                        result = questionResult,
                        question = localContentRepository.getStudyQuestion(
                            releaseId = result.summary.releaseId,
                            questionId = questionResult.questionId,
                            languageCode = result.summary.languageCode,
                        ),
                    )
                }
                ExamHistoryDetailUiState.Content(result, questions)
            }
        } catch (_: Exception) {
            ExamHistoryDetailUiState.Error
        }
    }

    ExamHistoryDetailScreen(
        uiState = uiState,
        onBack = onBack,
    )
}
