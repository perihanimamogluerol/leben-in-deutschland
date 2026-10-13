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
    ) : StudySessionUiState {
        val isLastQuestion: Boolean = currentIndex == totalCount - 1
        val isCorrect: Boolean = selectedAnswerId == question.correctAnswerId
    }

    data class Complete(
        val questionCount: Int,
    ) : StudySessionUiState

    data object Error : StudySessionUiState
}
