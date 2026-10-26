package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyAnswerProgress
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import java.util.Locale

data class StudySession(
    val releaseId: String,
    val languageCode: String,
    val stateCode: String?,
    val questionIds: List<String>,
    val startIndex: Int,
    val answeredCount: Int,
    val correctCount: Int,
)

class StudySessionCoordinator(
    private val localContentRepository: LocalContentRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun start(
        languageCode: String,
        stateCode: String?,
    ): StudySession {
        val release = requireNotNull(localContentRepository.getActiveRelease()) {
            "An active content release is required"
        }
        val normalizedStateCode = stateCode?.uppercase(Locale.ROOT)
        val questionIds = localContentRepository.getEligibleQuestionIds(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = normalizedStateCode,
        )
        require(questionIds.isNotEmpty()) {
            "No eligible questions are installed"
        }
        var progress = localContentRepository.getStudyAnswerProgress(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = normalizedStateCode,
        ).filter { answer -> answer.questionId in questionIds }
        if (progress.map(StudyAnswerProgress::questionId).toSet().size == questionIds.size) {
            localContentRepository.clearStudyProgress(
                releaseId = release.releaseId,
                languageCode = languageCode,
                stateCode = normalizedStateCode,
            )
            progress = emptyList()
        }
        val answeredQuestionIds = progress.map(StudyAnswerProgress::questionId).toSet()
        return StudySession(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = normalizedStateCode,
            questionIds = questionIds,
            startIndex = questionIds.indexOfFirst { questionId ->
                questionId !in answeredQuestionIds
            }.coerceAtLeast(0),
            answeredCount = answeredQuestionIds.size,
            correctCount = progress.count(StudyAnswerProgress::isCorrect),
        )
    }

    suspend fun getQuestion(
        session: StudySession,
        index: Int,
    ): StudyQuestion {
        val questionId = session.questionIds[index]
        return requireNotNull(
            localContentRepository.getStudyQuestion(
                releaseId = session.releaseId,
                questionId = questionId,
                languageCode = session.languageCode,
            ),
        ) { "Missing installed question: $questionId" }
    }

    suspend fun recordAnswer(
        session: StudySession,
        question: StudyQuestion,
        selectedAnswerId: String,
    ): StudyAnswerProgress {
        require(question.releaseId == session.releaseId)
        require(question.languageCode == session.languageCode)
        require(question.id in session.questionIds)
        require(question.answers.any { answer -> answer.id == selectedAnswerId })
        return localContentRepository.recordStudyAnswer(
            releaseId = session.releaseId,
            languageCode = session.languageCode,
            stateCode = session.stateCode,
            questionId = question.id,
            selectedAnswerId = selectedAnswerId,
            isCorrect = selectedAnswerId == question.correctAnswerId,
            answeredAtEpochMillis = currentTimeMillis(),
        )
    }
}
