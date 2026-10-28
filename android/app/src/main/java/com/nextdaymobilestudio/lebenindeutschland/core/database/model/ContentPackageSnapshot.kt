package com.nextdaymobilestudio.lebenindeutschland.core.database.model

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAnswerAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAuditEntity
import java.util.Locale

data class ContentPackageSnapshot(
    val release: ContentReleaseEntity,
    val releaseLanguages: List<ContentReleaseLanguageEntity>,
    val packageMetadata: ContentPackageEntity,
    val questions: List<QuestionEntity>,
    val answers: List<AnswerEntity>,
    val studyContent: List<StudyContentEntity>,
    val studyAnswers: List<StudyAnswerEntity>,
    val translationAudits: List<TranslationAuditEntity> = emptyList(),
    val translationAnswerAudits: List<TranslationAnswerAuditEntity> = emptyList(),
) {
    fun requireValid() {
        val releaseId = release.releaseId
        val languageCode = packageMetadata.languageCode
        val questionIds = questions.map(QuestionEntity::questionId).toSet()

        require(releaseId.isNotBlank()) { "releaseId must not be blank" }
        require(languageCode.isNotBlank()) { "languageCode must not be blank" }
        require(release.schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Only Schema $SUPPORTED_SCHEMA_VERSION releases are supported"
        }
        require(release.status == READY_STATUS && packageMetadata.status == READY_STATUS) {
            "Only READY content can be installed"
        }
        require(release.qualityIssueCount == 0 && packageMetadata.qualityIssueCount == 0) {
            "Content with quality issues cannot be installed"
        }
        require(packageMetadata.explanationReady) {
            "Package explanations must be ready before installation"
        }
        require(packageMetadata.releaseId == releaseId) { "Package releaseId does not match release" }
        require(packageMetadata.schemaVersion == release.schemaVersion) {
            "Package schemaVersion does not match release"
        }
        require(packageMetadata.questionCount == release.questionCount) {
            "Package questionCount does not match release"
        }
        require(packageMetadata.chunkSize == release.chunkSize) {
            "Package chunkSize does not match release"
        }
        require(packageMetadata.packageStrategy == release.packageStrategy) {
            "Package strategy does not match release"
        }
        require(releaseLanguages.size == release.languageCount) {
            "Release languageCount does not match release languages"
        }
        require(releaseLanguages.map(ContentReleaseLanguageEntity::languageCode).toSet().size ==
            releaseLanguages.size) { "Release language codes must be unique" }
        require(releaseLanguages.all { it.releaseId == releaseId }) {
            "Release language does not belong to this release"
        }
        require(releaseLanguages.any {
            it.languageCode == languageCode &&
                it.packageSha256 == packageMetadata.packageSha256
        }) { "Package hash does not match the release manifest" }
        require(questions.size == packageMetadata.questionCount) {
            "Package questionCount does not match questions"
        }
        require(questionIds.size == questions.size) { "Question IDs must be unique" }
        require(studyContent.size == questions.size) {
            "Every question must have study content for the package language"
        }

        require(questions.all { it.releaseId == releaseId }) { "Question releaseId mismatch" }
        require(answers.all { it.releaseId == releaseId && it.questionId in questionIds }) {
            "Answer does not belong to this release and question set"
        }
        require(studyContent.all {
            it.releaseId == releaseId &&
                it.languageCode == languageCode &&
                it.questionId in questionIds
        }) { "Study content does not belong to this package" }
        require(studyAnswers.all {
            it.releaseId == releaseId &&
                it.languageCode == languageCode &&
                it.questionId in questionIds
        }) { "Study answer does not belong to this package" }
        require(translationAudits.all {
            it.releaseId == releaseId &&
                it.languageCode == languageCode &&
                it.questionId in questionIds
        }) { "Translation audit does not belong to this package" }
        require(translationAnswerAudits.all {
            it.releaseId == releaseId &&
                it.languageCode == languageCode &&
                it.questionId in questionIds
        }) { "Translation answer audit does not belong to this package" }

        val answersByQuestion = answers.groupBy(AnswerEntity::questionId)
        val studyAnswersByQuestion = studyAnswers.groupBy(StudyAnswerEntity::questionId)
        val memoryKeyPairs = mutableSetOf<Pair<String, String>>()
        questions.forEach { question ->
            val germanAnswers = answersByQuestion.getValue(question.questionId)
            val localizedAnswers = studyAnswersByQuestion.getValue(question.questionId)
            require(germanAnswers.size == ANSWERS_PER_QUESTION) {
                "${question.questionId} must have $ANSWERS_PER_QUESTION German answers"
            }
            require(localizedAnswers.size == ANSWERS_PER_QUESTION) {
                "${question.questionId} must have $ANSWERS_PER_QUESTION study answers"
            }
            require(germanAnswers.map(AnswerEntity::answerId).toSet().size == ANSWERS_PER_QUESTION) {
                "${question.questionId} has duplicate German answer IDs"
            }
            require(localizedAnswers.map(StudyAnswerEntity::answerId).toSet() ==
                germanAnswers.map(AnswerEntity::answerId).toSet()) {
                "${question.questionId} study answer IDs do not match German answer IDs"
            }
            require(germanAnswers.any { it.answerId == question.correctAnswerId }) {
                "${question.questionId} correct answer is missing"
            }
            val memoryValues = listOf(
                question.memoryQuestionCue,
                question.memoryAnswerCue,
                question.memoryKeyStatus,
            )
            require(memoryValues.all { it == null } || memoryValues.all { !it.isNullOrBlank() }) {
                "${question.questionId} memory key must be complete or absent"
            }
            if (memoryValues.all { it != null }) {
                val correctAnswer = germanAnswers.single {
                    it.answerId == question.correctAnswerId
                }
                require(correctAnswer.imageSourcePath == null) {
                    "${question.questionId} image answers must not contain a memory key"
                }
                require(question.memoryKeyStatus in MEMORY_KEY_STATUSES) {
                    "${question.questionId} memory key status is not supported"
                }
                val pair = requireNotNull(question.memoryQuestionCue)
                    .normalizedMemoryCue() to requireNotNull(question.memoryAnswerCue)
                    .normalizedMemoryCue()
                require(memoryKeyPairs.add(pair)) {
                    "${question.questionId} memory key pair is duplicated"
                }
            }
        }

        val auditKeys = translationAudits.map { it.questionId to it.languageCode }.toSet()
        require(translationAnswerAudits.all { (it.questionId to it.languageCode) in auditKeys }) {
            "Translation answer audit is missing its parent audit"
        }
    }

    private companion object {
        const val ANSWERS_PER_QUESTION = 4
        const val READY_STATUS = "READY"
        const val SUPPORTED_SCHEMA_VERSION = 3
        val MEMORY_KEY_STATUSES = setOf(
            "AI_GENERATED_NEEDS_REVIEW",
            "HUMAN_REVIEWED",
        )
    }
}

private fun String.normalizedMemoryCue(): String =
    trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
