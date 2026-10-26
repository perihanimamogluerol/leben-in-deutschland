package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam

import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion

sealed interface MockExamUiState {
    data object Loading : MockExamUiState

    data object Setup : MockExamUiState

    data class Ready(
        val question: StudyQuestion,
        val currentIndex: Int,
        val totalCount: Int,
        val answeredCount: Int,
        val selectedAnswerId: String?,
        val remainingSeconds: Long,
        val showSubmitConfirmation: Boolean = false,
        val isSaving: Boolean = false,
    ) : MockExamUiState {
        val isFirstQuestion: Boolean = currentIndex == 0
        val isLastQuestion: Boolean = currentIndex == totalCount - 1
        val unansweredCount: Int = (totalCount - answeredCount).coerceAtLeast(0)
    }

    data class Result(
        val correctCount: Int,
        val wrongCount: Int,
        val unansweredCount: Int,
        val totalCount: Int,
        val submittedByTimeout: Boolean,
    ) : MockExamUiState {
        val lifeInGermanyPassed: Boolean =
            correctCount >= MockExamThresholds.LIFE_IN_GERMANY
        val naturalizationPassed: Boolean =
            correctCount >= MockExamThresholds.NATURALIZATION
    }

    data object Error : MockExamUiState
}
