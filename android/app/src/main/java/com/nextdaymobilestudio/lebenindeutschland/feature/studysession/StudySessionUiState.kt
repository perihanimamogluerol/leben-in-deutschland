package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion

sealed interface StudySessionUiState {
    data object Loading : StudySessionUiState

    data class Ready(
        val question: StudyQuestion,
        val currentIndex: Int,
        val totalCount: Int,
        val selectedAnswerId: String? = null,
        val isSubmitted: Boolean = false,
        val isSaving: Boolean = false,
        val answeredCount: Int = 0,
        val correctCount: Int = 0,
    ) : StudySessionUiState {
        val isLastQuestion: Boolean = currentIndex == totalCount - 1
        val isCorrect: Boolean = selectedAnswerId == question.correctAnswerId
    }

    data class Complete(
        val questionCount: Int,
        val correctCount: Int,
    ) : StudySessionUiState

    data object Error : StudySessionUiState
}
