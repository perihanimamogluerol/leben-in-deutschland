package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ui

object ExamHistoryTestTags {
    const val SCREEN = "exam-history-screen"
    const val BACK = "exam-history-back"
    const val EMPTY = "exam-history-empty"
    const val LIST = "exam-history-list"
    const val DETAIL_SCREEN = "exam-history-detail-screen"
    const val DETAIL_SUMMARY = "exam-history-detail-summary"
    const val QUESTION_LIST = "exam-history-question-list"

    fun attempt(attemptId: String): String = "exam-history-attempt-$attemptId"

    fun question(questionId: String): String = "exam-history-question-$questionId"
}
