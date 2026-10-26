package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory

import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamHistoryItem
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamQuestionResult
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamResultRecord
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion

sealed interface ExamHistoryUiState {
    data object Loading : ExamHistoryUiState

    data object Empty : ExamHistoryUiState

    data class Content(
        val attempts: List<MockExamHistoryItem>,
    ) : ExamHistoryUiState

    data object Error : ExamHistoryUiState
}

sealed interface ExamHistoryDetailUiState {
    data object Loading : ExamHistoryDetailUiState

    data class Content(
        val result: MockExamResultRecord,
        val questions: List<QuestionItem>,
    ) : ExamHistoryDetailUiState

    data object NotFound : ExamHistoryDetailUiState

    data object Error : ExamHistoryDetailUiState

    data class QuestionItem(
        val result: MockExamQuestionResult,
        val question: StudyQuestion?,
    )
}
