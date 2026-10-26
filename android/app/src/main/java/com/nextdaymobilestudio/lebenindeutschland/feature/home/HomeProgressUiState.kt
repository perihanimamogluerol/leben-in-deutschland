package com.nextdaymobilestudio.lebenindeutschland.feature.home

data class HomeProgressUiState(
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
) {
    val fraction: Float = if (totalCount == 0) {
        0f
    } else {
        answeredCount.toFloat() / totalCount.toFloat()
    }
    val isComplete: Boolean = totalCount > 0 && answeredCount >= totalCount
    val wrongCount: Int = (answeredCount - correctCount).coerceAtLeast(0)
    val hasWrongAnswers: Boolean = wrongCount > 0
}
