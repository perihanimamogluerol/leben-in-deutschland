package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mock_exam_attempts",
    indices = [
        Index(value = ["submitted_at_epoch_millis"]),
        Index(value = ["language_code", "state_code", "submitted_at_epoch_millis"]),
    ],
)
data class MockExamAttemptEntity(
    @PrimaryKey
    @ColumnInfo(name = "attempt_id")
    val attemptId: String,
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "state_code")
    val stateCode: String,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "submitted_at_epoch_millis")
    val submittedAtEpochMillis: Long,
    @ColumnInfo(name = "deadline_epoch_millis")
    val deadlineEpochMillis: Long,
    @ColumnInfo(name = "correct_count")
    val correctCount: Int,
    @ColumnInfo(name = "wrong_count")
    val wrongCount: Int,
    @ColumnInfo(name = "unanswered_count")
    val unansweredCount: Int,
    @ColumnInfo(name = "total_count")
    val totalCount: Int,
    @ColumnInfo(name = "submitted_by_timeout")
    val submittedByTimeout: Boolean,
)
