package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "study_content",
    primaryKeys = ["release_id", "question_id", "language_code"],
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
        Index(value = ["release_id", "language_code"]),
    ],
)
data class StudyContentEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "question_text")
    val questionText: String,
    val explanation: String,
    @ColumnInfo(name = "text_status")
    val textStatus: String?,
    @ColumnInfo(name = "translation_status")
    val translationStatus: String?,
    @ColumnInfo(name = "explanation_status")
    val explanationStatus: String,
    val reviewed: Boolean,
)

@Entity(
    tableName = "study_answers",
    primaryKeys = ["release_id", "question_id", "language_code", "answer_id"],
    foreignKeys = [
        ForeignKey(
            entity = StudyContentEntity::class,
            parentColumns = ["release_id", "question_id", "language_code"],
            childColumns = ["release_id", "question_id", "language_code"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["release_id", "question_id", "language_code"])],
)
data class StudyAnswerEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "answer_id")
    val answerId: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    val text: String,
)
