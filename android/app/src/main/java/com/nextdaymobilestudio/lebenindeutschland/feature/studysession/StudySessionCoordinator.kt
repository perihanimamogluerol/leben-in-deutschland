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
    val mode: StudySessionMode = StudySessionMode.ALL_QUESTIONS,
)

class StudySessionCoordinator(
    private val localContentRepository: LocalContentRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val shuffle: (List<String>) -> List<String> = { values -> values.shuffled() },
) {
    suspend fun start(
        languageCode: String,
        stateCode: String?,
        mode: StudySessionMode = StudySessionMode.ALL_QUESTIONS,
    ): StudySession {
        val release = requireNotNull(localContentRepository.getActiveRelease()) {
            "An active content release is required"
        }
        val normalizedStateCode = stateCode?.uppercase(Locale.ROOT)
        val eligibleQuestionIds = localContentRepository.getEligibleQuestionIds(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = normalizedStateCode,
        )
        require(eligibleQuestionIds.isNotEmpty()) {
            "No eligible questions are installed"
        }
        var progress = localContentRepository.getStudyAnswerProgress(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = normalizedStateCode,
        ).filter { answer -> answer.questionId in eligibleQuestionIds }
        if (
            mode == StudySessionMode.ALL_QUESTIONS &&
            progress.map(StudyAnswerProgress::questionId).toSet().size == eligibleQuestionIds.size
        ) {
            localContentRepository.clearStudyProgress(
                releaseId = release.releaseId,
                languageCode = languageCode,
                stateCode = normalizedStateCode,
            )
            progress = emptyList()
        }
        val progressByQuestionId = progress.associateBy(StudyAnswerProgress::questionId)
        val answeredQuestionIds = progress.map(StudyAnswerProgress::questionId).toSet()
        val (questionIds, startIndex) = when (mode) {
            StudySessionMode.ALL_QUESTIONS -> {
                val answered = eligibleQuestionIds.filter { questionId ->
                    questionId in answeredQuestionIds
                }
                val randomizedUnanswered = shuffle(
                    eligibleQuestionIds.filterNot { questionId ->
                        questionId in answeredQuestionIds
                    },
                )
                (answered + randomizedUnanswered) to answered.size
            }
            StudySessionMode.WRONG_ANSWERS -> shuffle(
                eligibleQuestionIds.filter { questionId ->
                    progressByQuestionId[questionId]?.isCorrect == false
                },
            ) to 0
        }
        return StudySession(
            releaseId = release.releaseId,
            languageCode = languageCode,
            stateCode = normalizedStateCode,
            questionIds = questionIds,
            startIndex = startIndex,
            answeredCount = if (mode == StudySessionMode.WRONG_ANSWERS) 0 else answeredQuestionIds.size,
            correctCount = if (mode == StudySessionMode.WRONG_ANSWERS) {
                0
            } else {
                progress.count(StudyAnswerProgress::isCorrect)
            },
            mode = mode,
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
