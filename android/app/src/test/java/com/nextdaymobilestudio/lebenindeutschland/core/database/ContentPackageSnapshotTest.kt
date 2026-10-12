package com.nextdaymobilestudio.lebenindeutschland.core.database

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentPackageSnapshotTest {
    @Test
    fun validSnapshotIsAccepted() {
        snapshot().requireValid()
    }

    @Test
    fun mismatchedLocalizedAnswerIdsAreRejected() {
        val snapshot = snapshot()
        val invalid = snapshot.copy(
            studyAnswers = snapshot.studyAnswers.dropLast(1) +
                snapshot.studyAnswers.last().copy(answerId = "E"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            invalid.requireValid()
        }
    }

    @Test
    fun reusedReleaseIdWithWrongPackageReleaseIsRejected() {
        val snapshot = snapshot()
        val invalid = snapshot.copy(
            packageMetadata = snapshot.packageMetadata.copy(releaseId = "other-release"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            invalid.requireValid()
        }
    }

    private fun snapshot(): ContentPackageSnapshot {
        val releaseId = "release-1"
        val questionId = "general_001"
        val answerIds = listOf("A", "B", "C", "D")
        return ContentPackageSnapshot(
            release = ContentReleaseEntity(
                releaseId = releaseId,
                schemaVersion = 3,
                datasetVersion = "2026-10-05.103327",
                packageStrategy = "SELF_CONTAINED_LANGUAGE_PACK",
                status = "READY",
                createdAt = "2026-10-05T10:33:27Z",
                sourceCatalogSha256 = "source-hash",
                catalogSha256 = "catalog-hash",
                questionCount = 1,
                chunkSize = 25,
                languageCount = 1,
                qualityIssueCount = 0,
                installedAtEpochMillis = 1L,
            ),
            releaseLanguages = listOf(
                ContentReleaseLanguageEntity(
                    releaseId = releaseId,
                    languageCode = "de",
                    displayOrder = 0,
                    packageSha256 = "package-hash",
                ),
            ),
            packageMetadata = ContentPackageEntity(
                releaseId = releaseId,
                languageCode = "de",
                schemaVersion = 3,
                packageStrategy = "SELF_CONTAINED_LANGUAGE_PACK",
                status = "READY",
                createdAt = "2026-10-05T10:33:27Z",
                packageSha256 = "package-hash",
                questionCount = 1,
                chunkCount = 1,
                chunkSize = 25,
                jsonBytes = 100L,
                qualityIssueCount = 0,
                explanationReady = true,
                installedAtEpochMillis = 1L,
            ),
            questions = listOf(question(releaseId, questionId)),
            answers = answerIds.mapIndexed { index, answerId ->
                answer(releaseId, questionId, answerId, index)
            },
            studyContent = listOf(studyContent(releaseId, questionId, "de")),
            studyAnswers = answerIds.mapIndexed { index, answerId ->
                StudyAnswerEntity(
                    releaseId = releaseId,
                    questionId = questionId,
                    languageCode = "de",
                    answerId = answerId,
                    displayOrder = index,
                    text = "Antwort $answerId",
                )
            },
        )
    }

    private fun question(releaseId: String, questionId: String) = QuestionEntity(
        releaseId = releaseId,
        questionId = questionId,
        sourceId = "general-001",
        officialNumber = 1,
        scope = "GENERAL",
        stateCode = null,
        category = null,
        categoryStatus = "UNASSIGNED",
        sourceVersion = "2026-10-05.103327",
        sourcePdfSha256 = "pdf-hash",
        sourcePage = 2,
        bamfInternalId = "18476",
        correctAnswerId = "D",
        germanQuestionText = "Frage",
        questionImageSourcePath = null,
        questionImageSourceUrl = null,
        questionImageStoragePath = null,
        questionImageSha256 = null,
        questionImageWidth = null,
        questionImageHeight = null,
        questionImageMimeType = null,
    )

    private fun answer(
        releaseId: String,
        questionId: String,
        answerId: String,
        displayOrder: Int,
    ) = AnswerEntity(
        releaseId = releaseId,
        questionId = questionId,
        answerId = answerId,
        displayOrder = displayOrder,
        germanText = "Antwort $answerId",
        imageSourcePath = null,
        imageSourceUrl = null,
        imageStoragePath = null,
        imageSha256 = null,
        imageWidth = null,
        imageHeight = null,
        imageMimeType = null,
    )

    private fun studyContent(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ) = StudyContentEntity(
        releaseId = releaseId,
        questionId = questionId,
        languageCode = languageCode,
        questionText = "Frage",
        explanation = "Erklärung",
        textStatus = "SOURCE",
        translationStatus = null,
        explanationStatus = "AI_GENERATED_NEEDS_REVIEW",
        reviewed = false,
    )
}
