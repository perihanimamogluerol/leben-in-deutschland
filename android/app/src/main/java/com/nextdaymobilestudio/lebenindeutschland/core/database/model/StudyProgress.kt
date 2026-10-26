package com.nextdaymobilestudio.lebenindeutschland.core.database.model

import androidx.room.ColumnInfo

data class StudyProgressSummary(
    @ColumnInfo(name = "answered_count")
    val answeredCount: Int,
    @ColumnInfo(name = "correct_count")
    val correctCount: Int,
)

data class StudyAnswerProgress(
    val questionId: String,
    val selectedAnswerId: String,
    val isCorrect: Boolean,
    val answeredAtEpochMillis: Long,
    val attemptCount: Int,
)
