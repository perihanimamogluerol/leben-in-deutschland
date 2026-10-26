package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "mock_exam_question_results",
    primaryKeys = ["attempt_id", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = MockExamAttemptEntity::class,
            parentColumns = ["attempt_id"],
            childColumns = ["attempt_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["attempt_id"])],
)
data class MockExamQuestionResultEntity(
    @ColumnInfo(name = "attempt_id")
    val attemptId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    val position: Int,
    @ColumnInfo(name = "selected_answer_id")
    val selectedAnswerId: String?,
    @ColumnInfo(name = "correct_answer_id")
    val correctAnswerId: String,
)
