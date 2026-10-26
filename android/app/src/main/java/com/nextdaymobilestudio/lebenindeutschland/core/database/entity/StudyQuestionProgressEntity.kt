package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "study_question_progress",
    primaryKeys = ["release_id", "language_code", "state_code", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["release_id", "question_id"],
            childColumns = ["release_id", "question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["release_id", "question_id"]),
        Index(value = ["release_id", "language_code", "state_code"]),
    ],
)
data class StudyQuestionProgressEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "state_code")
    val stateCode: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "selected_answer_id")
    val selectedAnswerId: String,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    @ColumnInfo(name = "answered_at_epoch_millis")
    val answeredAtEpochMillis: Long,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int,
)
