package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import java.util.Locale

data class StudySession(
    val releaseId: String,
    val languageCode: String,
    val questionIds: List<String>,
)

class StudySessionCoordinator(
    private val localContentRepository: LocalContentRepository,
) {
    suspend fun start(
        languageCode: String,
        stateCode: String?,
    ): StudySession {
        val release = requireNotNull(localContentRepository.getActiveRelease()) {
            "An active content release is required"
        }
        val questionIds = localContentRepository.getEligibleQuestionIds(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = stateCode?.uppercase(Locale.ROOT),
        )
        require(questionIds.isNotEmpty()) {
            "No eligible questions are installed"
        }
        return StudySession(
            releaseId = release.releaseId,
            languageCode = languageCode,
            questionIds = questionIds,
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
}
