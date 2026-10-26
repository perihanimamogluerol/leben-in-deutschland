package com.nextdaymobilestudio.lebenindeutschland.core.database.repository

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamHistoryItem
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamResultRecord
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswerProgress
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswerRecord
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyProgressSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface LocalContentRepository {
    fun observeActiveRelease(): Flow<ContentReleaseEntity?>

    fun observeInstalledPackages(releaseId: String): Flow<List<ContentPackageEntity>>

    suspend fun getActiveRelease(): ContentReleaseEntity?

    suspend fun getPackage(releaseId: String, languageCode: String): ContentPackageEntity?

    suspend fun replacePackage(snapshot: ContentPackageSnapshot)

    suspend fun activateRelease(releaseId: String, requiredLanguageCode: String)

    suspend fun deleteRelease(releaseId: String)

    suspend fun getEligibleQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<String>

    suspend fun getGeneralQuestionIds(
        releaseId: String,
        languageCode: String,
    ): List<String> = getEligibleQuestionIds(
        releaseId = releaseId,
        languageCode = languageCode,
        stateCode = null,
    )

    suspend fun getStateQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String,
    ): List<String> = getEligibleQuestionIds(
        releaseId = releaseId,
        languageCode = languageCode,
        stateCode = stateCode,
    ).filterNot { questionId -> questionId.startsWith("general_") }

    suspend fun getStudyQuestion(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): StudyQuestion?

    fun observeStudyProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): Flow<StudyProgressSummary> = flowOf(StudyProgressSummary(0, 0))

    suspend fun getStudyAnswerProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<StudyAnswerProgress> = emptyList()

    suspend fun recordStudyAnswer(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
        questionId: String,
        selectedAnswerId: String,
        isCorrect: Boolean,
        answeredAtEpochMillis: Long,
    ): StudyAnswerProgress = error("Study progress is not supported")

    suspend fun recordStudyAnswers(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
        answers: List<StudyAnswerRecord>,
        answeredAtEpochMillis: Long,
    ): List<StudyAnswerProgress> = answers.map { answer ->
        recordStudyAnswer(
            releaseId = releaseId,
            languageCode = languageCode,
            stateCode = stateCode,
            questionId = answer.questionId,
            selectedAnswerId = answer.selectedAnswerId,
            isCorrect = answer.isCorrect,
            answeredAtEpochMillis = answeredAtEpochMillis,
        )
    }

    suspend fun clearStudyProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ) = Unit

    fun observeMockExamHistory(): Flow<List<MockExamHistoryItem>> = flowOf(emptyList())

    suspend fun getMockExamResult(attemptId: String): MockExamResultRecord? = null

    suspend fun recordMockExamResult(
        result: MockExamResultRecord,
        updateStudyProgress: Boolean = true,
    ): Boolean {
        if (updateStudyProgress) {
            recordStudyAnswers(
                releaseId = result.summary.releaseId,
                languageCode = result.summary.languageCode,
                stateCode = result.summary.stateCode,
                answers = result.questionResults.mapNotNull { questionResult ->
                    questionResult.selectedAnswerId?.let { selectedAnswerId ->
                        StudyAnswerRecord(
                            questionId = questionResult.questionId,
                            selectedAnswerId = selectedAnswerId,
                            isCorrect = questionResult.isCorrect,
                        )
                    }
                },
                answeredAtEpochMillis = result.summary.submittedAtEpochMillis,
            )
        }
        return true
    }
}
