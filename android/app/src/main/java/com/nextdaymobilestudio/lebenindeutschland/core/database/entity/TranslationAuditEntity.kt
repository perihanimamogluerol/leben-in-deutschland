package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "translation_audits",
    primaryKeys = ["release_id", "question_id", "language_code"],
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
data class TranslationAuditEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    val provider: String?,
    @ColumnInfo(name = "source_record_number")
    val sourceRecordNumber: String?,
    @ColumnInfo(name = "source_record_id")
    val sourceRecordId: String?,
    @ColumnInfo(name = "source_commit")
    val sourceCommit: String?,
    @ColumnInfo(name = "custom_id")
    val customId: String?,
    val model: String?,
    @ColumnInfo(name = "generated_at")
    val generatedAt: String?,
    @ColumnInfo(name = "question_match_score")
    val questionMatchScore: Double?,
    @ColumnInfo(name = "answer_set_match_score")
    val answerSetMatchScore: Double?,
)

@Entity(
    tableName = "translation_answer_audits",
    primaryKeys = ["release_id", "question_id", "language_code", "answer_id"],
    foreignKeys = [
        ForeignKey(
            entity = TranslationAuditEntity::class,
            parentColumns = ["release_id", "question_id", "language_code"],
            childColumns = ["release_id", "question_id", "language_code"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["release_id", "question_id", "language_code"])],
)
data class TranslationAnswerAuditEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "answer_id")
    val answerId: String,
    @ColumnInfo(name = "match_score")
    val matchScore: Double?,
    @ColumnInfo(name = "used_fallback")
    val usedFallback: Boolean,
)
