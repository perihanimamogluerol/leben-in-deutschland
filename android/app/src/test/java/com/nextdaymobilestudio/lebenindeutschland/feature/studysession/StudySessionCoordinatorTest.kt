package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswer
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
            questionIds = listOf("general_001", "state_he_001"),
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
) : LocalContentRepository {
    private val activeReleaseFlow = MutableStateFlow(activeRelease)

    var lastEligibleRequest: EligibleRequest? = null
        private set
    var lastQuestionRequest: QuestionRequest? = null
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
