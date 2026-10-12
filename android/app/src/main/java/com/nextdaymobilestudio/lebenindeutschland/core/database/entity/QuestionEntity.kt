package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "questions",
    primaryKeys = ["release_id", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = ContentReleaseEntity::class,
            parentColumns = ["release_id"],
            childColumns = ["release_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("release_id"),
        Index(value = ["release_id", "scope", "state_code"]),
        Index(value = ["release_id", "official_number"]),
    ],
)
data class QuestionEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    @ColumnInfo(name = "official_number")
    val officialNumber: Int,
    val scope: String,
    @ColumnInfo(name = "state_code")
    val stateCode: String?,
    val category: String?,
    @ColumnInfo(name = "category_status")
    val categoryStatus: String,
    @ColumnInfo(name = "source_version")
    val sourceVersion: String,
    @ColumnInfo(name = "source_pdf_sha256")
    val sourcePdfSha256: String,
    @ColumnInfo(name = "source_page")
    val sourcePage: Int,
    @ColumnInfo(name = "bamf_internal_id")
    val bamfInternalId: String,
    @ColumnInfo(name = "correct_answer_id")
    val correctAnswerId: String,
    @ColumnInfo(name = "german_question_text")
    val germanQuestionText: String,
    @ColumnInfo(name = "question_image_source_path")
    val questionImageSourcePath: String?,
    @ColumnInfo(name = "question_image_source_url")
    val questionImageSourceUrl: String?,
    @ColumnInfo(name = "question_image_storage_path")
    val questionImageStoragePath: String?,
    @ColumnInfo(name = "question_image_sha256")
    val questionImageSha256: String?,
    @ColumnInfo(name = "question_image_width")
    val questionImageWidth: Int?,
    @ColumnInfo(name = "question_image_height")
    val questionImageHeight: Int?,
    @ColumnInfo(name = "question_image_mime_type")
    val questionImageMimeType: String?,
)

@Entity(
    tableName = "answers",
    primaryKeys = ["release_id", "question_id", "answer_id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["release_id", "question_id"],
            childColumns = ["release_id", "question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["release_id", "question_id"])],
)
data class AnswerEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "answer_id")
    val answerId: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @ColumnInfo(name = "german_text")
    val germanText: String,
    @ColumnInfo(name = "image_source_path")
    val imageSourcePath: String?,
    @ColumnInfo(name = "image_source_url")
    val imageSourceUrl: String?,
    @ColumnInfo(name = "image_storage_path")
    val imageStoragePath: String?,
    @ColumnInfo(name = "image_sha256")
    val imageSha256: String?,
    @ColumnInfo(name = "image_width")
    val imageWidth: Int?,
    @ColumnInfo(name = "image_height")
    val imageHeight: Int?,
    @ColumnInfo(name = "image_mime_type")
    val imageMimeType: String?,
)
