package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswer
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswerProgress
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionCoordinatorTest {
    @Test
    fun startBuildsSessionFromActiveReleaseAndSelectedState() = runBlocking {
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf("general_001", "state_he_001"),
        )
        val coordinator = StudySessionCoordinator(repository)

        val session = coordinator.start(languageCode = "tr", stateCode = "he")

        assertEquals(RELEASE_ID, session.releaseId)
        assertEquals("tr", session.languageCode)
        assertEquals(listOf("general_001", "state_he_001"), session.questionIds)
        assertEquals(EligibleRequest(RELEASE_ID, "tr", "HE"), repository.lastEligibleRequest)
    }

    @Test
    fun getQuestionLoadsTheRequestedInstalledTranslation() = runBlocking {
        val expectedQuestion = studyQuestion("state_he_001")
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf("general_001", "state_he_001"),
            questions = mapOf("state_he_001" to expectedQuestion),
        )
        val coordinator = StudySessionCoordinator(repository)
        val session = StudySession(
            releaseId = RELEASE_ID,
            languageCode = "tr",
            stateCode = "HE",
            questionIds = listOf("general_001", "state_he_001"),
            startIndex = 0,
            answeredCount = 0,
            correctCount = 0,
        )

        val question = coordinator.getQuestion(session, index = 1)

        assertEquals(expectedQuestion, question)
        assertEquals(QuestionRequest(RELEASE_ID, "state_he_001", "tr"), repository.lastQuestionRequest)
    }

    @Test
    fun startRejectsMissingActiveContent() {
        val coordinator = StudySessionCoordinator(
            FakeStudyRepository(activeRelease = null, eligibleIds = emptyList()),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { coordinator.start(languageCode = "de", stateCode = "bw") }
        }
    }

    @Test
    fun startResumesAtFirstUnansweredQuestion() = runBlocking {
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf("general_001", "general_002", "state_he_001"),
            progress = listOf(
                StudyAnswerProgress("general_001", "A", true, 100L, 1),
                StudyAnswerProgress("general_002", "B", false, 200L, 1),
            ),
        )

        val session = StudySessionCoordinator(repository).start("tr", "he")

        assertEquals(2, session.startIndex)
        assertEquals(2, session.answeredCount)
        assertEquals(1, session.correctCount)
    }

    @Test
    fun recordAnswerPersistsCorrectnessAndTimestamp() = runBlocking {
        val question = studyQuestion("state_he_001")
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf(question.id),
            questions = mapOf(question.id to question),
        )
        val coordinator = StudySessionCoordinator(repository, currentTimeMillis = { 456L })
        val session = coordinator.start("tr", "he")

        val saved = coordinator.recordAnswer(session, question, selectedAnswerId = "A")

        assertEquals(true, saved.isCorrect)
        assertEquals(456L, saved.answeredAtEpochMillis)
        assertEquals(saved, repository.recordedProgress)
    }

    @Test
    fun completedProgressIsClearedBeforeStartingAgain() = runBlocking {
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf("general_001", "state_he_001"),
            progress = listOf(
                StudyAnswerProgress("general_001", "A", true, 100L, 1),
                StudyAnswerProgress("state_he_001", "B", false, 200L, 1),
            ),
        )

        val session = StudySessionCoordinator(repository).start("tr", "he")

        assertEquals(1, repository.clearCount)
        assertEquals(0, session.startIndex)
        assertEquals(0, session.answeredCount)
        assertEquals(0, session.correctCount)
    }

    @Test
    fun wrongAnswersModeContainsOnlyCurrentlyIncorrectQuestions() = runBlocking {
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf("general_001", "general_002", "state_he_001"),
            progress = listOf(
                StudyAnswerProgress("general_001", "A", true, 100L, 1),
                StudyAnswerProgress("general_002", "B", false, 200L, 1),
            ),
        )

        val session = StudySessionCoordinator(repository).start(
            languageCode = "tr",
            stateCode = "he",
            mode = StudySessionMode.WRONG_ANSWERS,
        )

        assertEquals(listOf("general_002"), session.questionIds)
        assertEquals(0, session.startIndex)
        assertEquals(0, session.answeredCount)
        assertEquals(0, repository.clearCount)
    }

    @Test
    fun wrongAnswersModeIsEmptyWhenEveryAnsweredQuestionIsCorrect() = runBlocking {
        val repository = FakeStudyRepository(
            activeRelease = releaseEntity(),
            eligibleIds = listOf("general_001", "general_002"),
            progress = listOf(
                StudyAnswerProgress("general_001", "A", true, 100L, 1),
            ),
        )

        val session = StudySessionCoordinator(repository).start(
            languageCode = "tr",
            stateCode = "he",
            mode = StudySessionMode.WRONG_ANSWERS,
        )

        assertTrue(session.questionIds.isEmpty())
        assertEquals(0, repository.clearCount)
    }
}

private data class EligibleRequest(
    val releaseId: String,
    val languageCode: String,
    val stateCode: String?,
)

private data class QuestionRequest(
    val releaseId: String,
    val questionId: String,
    val languageCode: String,
)

private class FakeStudyRepository(
    activeRelease: ContentReleaseEntity?,
    private val eligibleIds: List<String>,
    private val questions: Map<String, StudyQuestion> = emptyMap(),
    private val progress: List<StudyAnswerProgress> = emptyList(),
) : LocalContentRepository {
    private val activeReleaseFlow = MutableStateFlow(activeRelease)

    var lastEligibleRequest: EligibleRequest? = null
        private set
    var lastQuestionRequest: QuestionRequest? = null
        private set
    var recordedProgress: StudyAnswerProgress? = null
        private set
    var clearCount: Int = 0
        private set

    override fun observeActiveRelease(): Flow<ContentReleaseEntity?> = activeReleaseFlow

    override fun observeInstalledPackages(releaseId: String): Flow<List<ContentPackageEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun getActiveRelease(): ContentReleaseEntity? = activeReleaseFlow.value

    override suspend fun getPackage(
        releaseId: String,
        languageCode: String,
    ): ContentPackageEntity? = null

    override suspend fun replacePackage(snapshot: ContentPackageSnapshot) = Unit

    override suspend fun activateRelease(releaseId: String, requiredLanguageCode: String) = Unit

    override suspend fun deleteRelease(releaseId: String) = Unit

    override suspend fun getEligibleQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<String> {
        lastEligibleRequest = EligibleRequest(releaseId, languageCode, stateCode)
        return eligibleIds
    }

    override suspend fun getStudyQuestion(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): StudyQuestion? {
        lastQuestionRequest = QuestionRequest(releaseId, questionId, languageCode)
        return questions[questionId]
    }

    override suspend fun getStudyAnswerProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<StudyAnswerProgress> = progress

    override suspend fun recordStudyAnswer(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
        questionId: String,
        selectedAnswerId: String,
        isCorrect: Boolean,
        answeredAtEpochMillis: Long,
    ): StudyAnswerProgress = StudyAnswerProgress(
        questionId = questionId,
        selectedAnswerId = selectedAnswerId,
        isCorrect = isCorrect,
        answeredAtEpochMillis = answeredAtEpochMillis,
        attemptCount = 1,
    ).also { saved -> recordedProgress = saved }

    override suspend fun clearStudyProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ) {
        clearCount += 1
    }
}

private fun releaseEntity(): ContentReleaseEntity = ContentReleaseEntity(
    releaseId = RELEASE_ID,
    schemaVersion = 3,
    datasetVersion = "2026-10",
    packageStrategy = "SELF_CONTAINED_LANGUAGE_PACK",
    status = "READY",
    createdAt = "2026-10-12T00:00:00Z",
    sourceCatalogSha256 = "a".repeat(64),
    catalogSha256 = "b".repeat(64),
    questionCount = 460,
    chunkSize = 10,
    languageCount = 21,
    qualityIssueCount = 0,
    installedAtEpochMillis = 123L,
    isActive = true,
)

private fun studyQuestion(questionId: String): StudyQuestion = StudyQuestion(
    releaseId = RELEASE_ID,
    id = questionId,
    officialNumber = 1,
    scope = "STATE",
    stateCode = "he",
    category = null,
    correctAnswerId = "A",
    germanQuestionText = "Was ist richtig?",
    localizedQuestionText = "Hangisi doğrudur?",
    explanation = "Erklärung",
    languageCode = "tr",
    questionImage = null,
    answers = listOf(
        StudyAnswer("A", 0, "Antwort A", "Cevap A", null),
        StudyAnswer("B", 1, "Antwort B", "Cevap B", null),
    ),
    explanationStatus = "AI_GENERATED_NEEDS_REVIEW",
    reviewed = false,
)

private const val RELEASE_ID = "release-3"
