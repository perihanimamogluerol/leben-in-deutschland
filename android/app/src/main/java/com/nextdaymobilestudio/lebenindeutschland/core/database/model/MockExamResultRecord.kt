package com.nextdaymobilestudio.lebenindeutschland.core.database.model

data class MockExamHistoryItem(
    val attemptId: String,
    val releaseId: String,
    val languageCode: String,
    val stateCode: String,
    val startedAtEpochMillis: Long,
    val submittedAtEpochMillis: Long,
    val deadlineEpochMillis: Long,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val totalCount: Int,
    val submittedByTimeout: Boolean,
) {
    val durationSeconds: Long =
        (minOf(submittedAtEpochMillis, deadlineEpochMillis) - startedAtEpochMillis)
            .coerceAtLeast(0L) / 1_000L

    val lifeInGermanyPassed: Boolean = correctCount >= 15

    val naturalizationPassed: Boolean = correctCount >= 17
}

data class MockExamQuestionResult(
    val questionId: String,
    val position: Int,
    val selectedAnswerId: String?,
    val correctAnswerId: String,
) {
    val isAnswered: Boolean = selectedAnswerId != null

    val isCorrect: Boolean = selectedAnswerId == correctAnswerId
}

data class MockExamResultRecord(
    val summary: MockExamHistoryItem,
    val questionResults: List<MockExamQuestionResult>,
) {
    init {
        require(summary.totalCount > 0)
        require(
            summary.correctCount + summary.wrongCount + summary.unansweredCount ==
                summary.totalCount,
        )
        require(questionResults.size == summary.totalCount)
        require(
            questionResults.map(MockExamQuestionResult::questionId).distinct().size ==
                summary.totalCount,
        )
        require(
            questionResults.map(MockExamQuestionResult::position).sorted() ==
                questionResults.indices.toList(),
        )
        require(questionResults.count(MockExamQuestionResult::isCorrect) == summary.correctCount)
        require(
            questionResults.count { result -> result.isAnswered && !result.isCorrect } ==
                summary.wrongCount,
        )
        require(
            questionResults.count { result -> !result.isAnswered } == summary.unansweredCount,
        )
    }
}
