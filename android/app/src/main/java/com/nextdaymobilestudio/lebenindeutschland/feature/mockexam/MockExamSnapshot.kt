package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam

import kotlinx.serialization.Serializable

@Serializable
data class MockExamSnapshot(
    val releaseId: String,
    val languageCode: String,
    val stateCode: String,
    val questionIds: List<String>,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val currentIndex: Int = 0,
    val startedAtEpochMillis: Long,
    val deadlineEpochMillis: Long,
    val submittedAtEpochMillis: Long? = null,
    val correctCount: Int? = null,
    val submittedByTimeout: Boolean = false,
) {
    val attemptId: String
        get() = "$releaseId:$languageCode:$stateCode:$startedAtEpochMillis"

    val isComplete: Boolean = submittedAtEpochMillis != null
    val answeredCount: Int = selectedAnswers.size
    val unansweredCount: Int = (questionIds.size - answeredCount).coerceAtLeast(0)
    val wrongCount: Int
        get() = if (isComplete) {
            (questionIds.size - (correctCount ?: 0) - unansweredCount).coerceAtLeast(0)
        } else {
            0
        }
}
