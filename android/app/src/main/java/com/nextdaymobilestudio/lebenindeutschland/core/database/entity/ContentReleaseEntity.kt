package com.nextdaymobilestudio.lebenindeutschland.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "content_releases")
data class ContentReleaseEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String,
    @ColumnInfo(name = "package_strategy")
    val packageStrategy: String,
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "source_catalog_sha256")
    val sourceCatalogSha256: String,
    @ColumnInfo(name = "catalog_sha256")
    val catalogSha256: String,
    @ColumnInfo(name = "question_count")
    val questionCount: Int,
    @ColumnInfo(name = "chunk_size")
    val chunkSize: Int,
    @ColumnInfo(name = "language_count")
    val languageCount: Int,
    @ColumnInfo(name = "quality_issue_count")
    val qualityIssueCount: Int,
    @ColumnInfo(name = "installed_at_epoch_millis")
    val installedAtEpochMillis: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,
)

@Entity(
    tableName = "content_release_languages",
    primaryKeys = ["release_id", "language_code"],
    foreignKeys = [
        ForeignKey(
            entity = ContentReleaseEntity::class,
            parentColumns = ["release_id"],
            childColumns = ["release_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("release_id")],
)
data class ContentReleaseLanguageEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @ColumnInfo(name = "package_sha256")
    val packageSha256: String,
)

@Entity(
    tableName = "content_packages",
    primaryKeys = ["release_id", "language_code"],
    foreignKeys = [
        ForeignKey(
            entity = ContentReleaseEntity::class,
            parentColumns = ["release_id"],
            childColumns = ["release_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("release_id")],
)
data class ContentPackageEntity(
    @ColumnInfo(name = "release_id")
    val releaseId: String,
    @ColumnInfo(name = "language_code")
    val languageCode: String,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
    @ColumnInfo(name = "package_strategy")
    val packageStrategy: String,
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "package_sha256")
    val packageSha256: String,
    @ColumnInfo(name = "question_count")
    val questionCount: Int,
    @ColumnInfo(name = "chunk_count")
    val chunkCount: Int,
    @ColumnInfo(name = "chunk_size")
    val chunkSize: Int,
    @ColumnInfo(name = "json_bytes")
    val jsonBytes: Long,
    @ColumnInfo(name = "quality_issue_count")
    val qualityIssueCount: Int,
    @ColumnInfo(name = "explanation_ready")
    val explanationReady: Boolean,
    @ColumnInfo(name = "installed_at_epoch_millis")
    val installedAtEpochMillis: Long,
)
