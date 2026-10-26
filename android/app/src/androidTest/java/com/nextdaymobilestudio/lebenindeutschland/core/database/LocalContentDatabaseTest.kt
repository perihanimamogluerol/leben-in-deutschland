package com.nextdaymobilestudio.lebenindeutschland.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAnswerAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamHistoryItem
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamQuestionResult
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamResultRecord
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswerRecord
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.RoomLocalContentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalContentDatabaseTest {
    private lateinit var database: LocalContentDatabase
    private lateinit var repository: RoomLocalContentRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            LocalContentDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomLocalContentRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun packageInstallActivationAndStateFilteringWorkTogether() = runBlocking {
        repository.replacePackage(snapshot(languageCode = "de"))
        repository.replacePackage(snapshot(languageCode = "en"))
        repository.activateRelease(RELEASE_ID, "en")

        assertEquals(RELEASE_ID, repository.observeActiveRelease().first()?.releaseId)
        assertEquals(
            listOf("general_001", "state_be_001"),
            repository.getEligibleQuestionIds(RELEASE_ID, "en", "BE"),
        )
        assertEquals(
            listOf("general_001", "state_by_001"),
            repository.getEligibleQuestionIds(RELEASE_ID, "en", "BY"),
        )
        assertEquals(
            listOf("general_001"),
            repository.getEligibleQuestionIds(RELEASE_ID, "en", null),
        )

        val question = repository.getStudyQuestion(RELEASE_ID, "general_001", "en")
        assertNotNull(question)
        assertEquals("English question general_001", question?.localizedQuestionText)
        assertEquals("English answer D", question?.answers?.last()?.localizedText)
        assertEquals("D", question?.correctAnswerId)
        assertEquals("image/jpeg", question?.questionImage?.mimeType)
        assertEquals("answer-D.png", question?.answers?.last()?.image?.sourcePath)

        val audit = database.contentDao().getTranslationAudit(
            RELEASE_ID,
            "general_001",
            "en",
        )
        assertEquals("source-record-general_001", audit?.sourceRecordId)
        assertEquals(
            4,
            database.contentDao().getTranslationAnswerAudits(
                RELEASE_ID,
                "general_001",
                "en",
            ).size,
        )
    }

    @Test
    fun replacingOneLanguageIsAtomicAndKeepsOtherPackages() = runBlocking {
        repository.replacePackage(snapshot(languageCode = "de"))
        repository.replacePackage(snapshot(languageCode = "en"))

        val updated = snapshot(languageCode = "en", explanationSuffix = " updated")
        repository.replacePackage(updated)

        assertNotNull(database.contentDao().getPackage(RELEASE_ID, "de"))
        assertNotNull(database.contentDao().getPackage(RELEASE_ID, "en"))
        assertEquals(
            "English explanation general_001 updated",
            repository.getStudyQuestion(RELEASE_ID, "general_001", "en")?.explanation,
        )
    }

    @Test
    fun activationRequiresGermanAndSelectedLanguagePackages() {
        runBlocking {
            repository.replacePackage(snapshot(languageCode = "en"))
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.activateRelease(RELEASE_ID, "en") }
        }
    }

    @Test
    fun activeReleaseCannotBeDeleted() = runBlocking {
        repository.replacePackage(snapshot(languageCode = "de"))
        repository.activateRelease(RELEASE_ID, "de")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.deleteRelease(RELEASE_ID) }
        }
        assertFalse(repository.observeInstalledPackages(RELEASE_ID).first().isEmpty())
    }

    @Test
    fun studyProgressPersistsAttemptsAndCanBeCleared() = runBlocking {
        repository.replacePackage(snapshot(languageCode = "de"))
        repository.replacePackage(snapshot(languageCode = "en"))
        repository.activateRelease(RELEASE_ID, "en")

        repository.recordStudyAnswer(
            releaseId = RELEASE_ID,
            languageCode = "en",
            stateCode = "be",
            questionId = "general_001",
            selectedAnswerId = "A",
            isCorrect = false,
            answeredAtEpochMillis = 100L,
        )
        val secondAttempt = repository.recordStudyAnswers(
            releaseId = RELEASE_ID,
            languageCode = "en",
            stateCode = "BE",
            answers = listOf(
                StudyAnswerRecord(
                    questionId = "general_001",
                    selectedAnswerId = "D",
                    isCorrect = true,
                ),
            ),
            answeredAtEpochMillis = 200L,
        ).single()

        assertEquals(2, secondAttempt.attemptCount)
        assertEquals("D", secondAttempt.selectedAnswerId)
        assertEquals(
            1,
            repository.observeStudyProgress(RELEASE_ID, "en", "be")
                .first().answeredCount,
        )
        assertEquals(
            1,
            repository.observeStudyProgress(RELEASE_ID, "en", "BE")
                .first().correctCount,
        )
        assertEquals(
            1,
            repository.getStudyAnswerProgress(RELEASE_ID, "en", "be").size,
        )

        repository.clearStudyProgress(RELEASE_ID, "en", "be")

        assertEquals(
            0,
            repository.observeStudyProgress(RELEASE_ID, "en", "BE")
                .first().answeredCount,
        )
    }

    @Test
    fun mockExamHistoryIsStoredAtomicallyAndIdempotently() = runBlocking {
        repository.replacePackage(snapshot(languageCode = "de"))
        repository.replacePackage(snapshot(languageCode = "en"))
        repository.activateRelease(RELEASE_ID, "en")
        val result = MockExamResultRecord(
            summary = MockExamHistoryItem(
                attemptId = "attempt-1",
                releaseId = RELEASE_ID,
                languageCode = "en",
                stateCode = "BE",
                startedAtEpochMillis = 100L,
                submittedAtEpochMillis = 200L,
                deadlineEpochMillis = 3_600_100L,
                correctCount = 1,
                wrongCount = 0,
                unansweredCount = 1,
                totalCount = 2,
                submittedByTimeout = false,
            ),
            questionResults = listOf(
                MockExamQuestionResult(
                    questionId = "general_001",
                    position = 0,
                    selectedAnswerId = "D",
                    correctAnswerId = "D",
                ),
                MockExamQuestionResult(
                    questionId = "state_be_001",
                    position = 1,
                    selectedAnswerId = null,
                    correctAnswerId = "D",
                ),
            ),
        )

        assertTrue(repository.recordMockExamResult(result))
        assertFalse(repository.recordMockExamResult(result))

        val history = repository.observeMockExamHistory().first()
        assertEquals(listOf("attempt-1"), history.map(MockExamHistoryItem::attemptId))
        assertEquals(result, repository.getMockExamResult("attempt-1"))
        assertEquals(
            1,
            repository.getStudyAnswerProgress(RELEASE_ID, "en", "BE")
                .single().attemptCount,
        )
    }

    private fun snapshot(
        languageCode: String,
        explanationSuffix: String = "",
    ): ContentPackageSnapshot {
        val questions = listOf(
            question("general_001", 1, "GENERAL", null),
            question("state_be_001", 301, "STATE", "BE"),
            question("state_by_001", 301, "STATE", "BY"),
        )
        val languageName = if (languageCode == "de") "German" else "English"
        return ContentPackageSnapshot(
            release = release(),
            releaseLanguages = listOf("de", "en").mapIndexed { index, code ->
                ContentReleaseLanguageEntity(
                    releaseId = RELEASE_ID,
                    languageCode = code,
                    displayOrder = index,
                    packageSha256 = "package-$code",
                )
            },
            packageMetadata = ContentPackageEntity(
                releaseId = RELEASE_ID,
                languageCode = languageCode,
                schemaVersion = 3,
                packageStrategy = "SELF_CONTAINED_LANGUAGE_PACK",
                status = "READY",
                createdAt = CREATED_AT,
                packageSha256 = "package-$languageCode",
                questionCount = questions.size,
                chunkCount = 1,
                chunkSize = 25,
                jsonBytes = 1_000L,
                qualityIssueCount = 0,
                explanationReady = true,
                installedAtEpochMillis = 1L,
            ),
            questions = questions,
            answers = questions.flatMap { question ->
                ANSWER_IDS.mapIndexed { index, answerId ->
                    AnswerEntity(
                        releaseId = RELEASE_ID,
                        questionId = question.questionId,
                        answerId = answerId,
                        displayOrder = index,
                        germanText = "German answer $answerId",
                        imageSourcePath = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            "answer-D.png"
                        } else {
                            null
                        },
                        imageSourceUrl = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            "https://example.com/answer-D.png"
                        } else {
                            null
                        },
                        imageStoragePath = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            "question-images/answer-D.png"
                        } else {
                            null
                        },
                        imageSha256 = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            "answer-image-hash"
                        } else {
                            null
                        },
                        imageWidth = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            375
                        } else {
                            null
                        },
                        imageHeight = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            488
                        } else {
                            null
                        },
                        imageMimeType = if (
                            question.questionId == "general_001" && answerId == "D"
                        ) {
                            "image/png"
                        } else {
                            null
                        },
                    )
                }
            },
            studyContent = questions.map { question ->
                StudyContentEntity(
                    releaseId = RELEASE_ID,
                    questionId = question.questionId,
                    languageCode = languageCode,
                    questionText = "$languageName question ${question.questionId}",
                    explanation = "$languageName explanation ${question.questionId}$explanationSuffix",
                    textStatus = if (languageCode == "de") "SOURCE" else null,
                    translationStatus = if (languageCode == "de") null else "AI_GENERATED_NEEDS_REVIEW",
                    explanationStatus = "AI_GENERATED_NEEDS_REVIEW",
                    reviewed = false,
                )
            },
            studyAnswers = questions.flatMap { question ->
                ANSWER_IDS.mapIndexed { index, answerId ->
                    StudyAnswerEntity(
                        releaseId = RELEASE_ID,
                        questionId = question.questionId,
                        languageCode = languageCode,
                        answerId = answerId,
                        displayOrder = index,
                        text = "$languageName answer $answerId",
                    )
                }
            },
            translationAudits = if (languageCode == "de") {
                emptyList()
            } else {
                questions.map { question ->
                    TranslationAuditEntity(
                        releaseId = RELEASE_ID,
                        questionId = question.questionId,
                        languageCode = languageCode,
                        provider = "test-provider",
                        sourceRecordNumber = "1",
                        sourceRecordId = "source-record-${question.questionId}",
                        sourceCommit = "source-commit",
                        customId = null,
                        model = null,
                        generatedAt = null,
                        questionMatchScore = 1.0,
                        answerSetMatchScore = 1.0,
                    )
                }
            },
            translationAnswerAudits = if (languageCode == "de") {
                emptyList()
            } else {
                questions.flatMap { question ->
                    ANSWER_IDS.map { answerId ->
                        TranslationAnswerAuditEntity(
                            releaseId = RELEASE_ID,
                            questionId = question.questionId,
                            languageCode = languageCode,
                            answerId = answerId,
                            matchScore = 1.0,
                            usedFallback = false,
                        )
                    }
                }
            },
        )
    }

    private fun release() = ContentReleaseEntity(
        releaseId = RELEASE_ID,
        schemaVersion = 3,
        datasetVersion = "2026-10-05.103327",
        packageStrategy = "SELF_CONTAINED_LANGUAGE_PACK",
        status = "READY",
        createdAt = CREATED_AT,
        sourceCatalogSha256 = "source-hash",
        catalogSha256 = "catalog-hash",
        questionCount = 3,
        chunkSize = 25,
        languageCount = 2,
        qualityIssueCount = 0,
        installedAtEpochMillis = 1L,
    )

    private fun question(
        questionId: String,
        officialNumber: Int,
        scope: String,
        stateCode: String?,
    ) = QuestionEntity(
        releaseId = RELEASE_ID,
        questionId = questionId,
        sourceId = questionId.replace('_', '-'),
        officialNumber = officialNumber,
        scope = scope,
        stateCode = stateCode,
        category = null,
        categoryStatus = "UNASSIGNED",
        sourceVersion = "2026-10-05.103327",
        sourcePdfSha256 = "pdf-hash",
        sourcePage = 2,
        bamfInternalId = questionId,
        correctAnswerId = "D",
        germanQuestionText = "German question $questionId",
        questionImageSourcePath = if (questionId == "general_001") "question.jpeg" else null,
        questionImageSourceUrl = if (questionId == "general_001") {
            "https://example.com/question.jpeg"
        } else {
            null
        },
        questionImageStoragePath = if (questionId == "general_001") {
            "question-images/question.jpeg"
        } else {
            null
        },
        questionImageSha256 = if (questionId == "general_001") "question-image-hash" else null,
        questionImageWidth = if (questionId == "general_001") 1200 else null,
        questionImageHeight = if (questionId == "general_001") 857 else null,
        questionImageMimeType = if (questionId == "general_001") "image/jpeg" else null,
    )

    private companion object {
        const val RELEASE_ID = "lid-schema-3"
        const val CREATED_AT = "2026-10-05T10:33:27Z"
        val ANSWER_IDS = listOf("A", "B", "C", "D")
    }
}
