package com.nextdaymobilestudio.lebenindeutschland.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.MockExamAttemptEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.MockExamQuestionResultEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyQuestionProgressEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAnswerAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.LocalQuestionBundle
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyProgressSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Upsert
    suspend fun upsertRelease(release: ContentReleaseEntity)

    @Upsert
    suspend fun upsertReleaseLanguages(languages: List<ContentReleaseLanguageEntity>)

    @Upsert
    suspend fun upsertPackage(packageMetadata: ContentPackageEntity)

    @Upsert
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Upsert
    suspend fun upsertAnswers(answers: List<AnswerEntity>)

    @Upsert
    suspend fun upsertStudyContent(studyContent: List<StudyContentEntity>)

    @Upsert
    suspend fun upsertStudyAnswers(studyAnswers: List<StudyAnswerEntity>)

    @Upsert
    suspend fun upsertTranslationAudits(audits: List<TranslationAuditEntity>)

    @Upsert
    suspend fun upsertTranslationAnswerAudits(audits: List<TranslationAnswerAuditEntity>)

    @Upsert
    suspend fun upsertStudyQuestionProgress(progress: StudyQuestionProgressEntity)

    @Upsert
    suspend fun upsertMockExamAttempt(attempt: MockExamAttemptEntity)

    @Upsert
    suspend fun upsertMockExamQuestionResults(results: List<MockExamQuestionResultEntity>)

    @Query(
        """
        DELETE FROM study_content
        WHERE release_id = :releaseId AND language_code = :languageCode
        """,
    )
    suspend fun deleteStudyContent(releaseId: String, languageCode: String)

    @Query(
        """
        DELETE FROM content_packages
        WHERE release_id = :releaseId AND language_code = :languageCode
        """,
    )
    suspend fun deletePackageMetadata(releaseId: String, languageCode: String)

    @Query("SELECT * FROM content_releases WHERE release_id = :releaseId")
    suspend fun getRelease(releaseId: String): ContentReleaseEntity?

    @Query("SELECT * FROM content_releases WHERE is_active = 1 LIMIT 1")
    fun observeActiveRelease(): Flow<ContentReleaseEntity?>

    @Query("SELECT * FROM content_releases WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveRelease(): ContentReleaseEntity?

    @Query("SELECT * FROM content_packages WHERE release_id = :releaseId ORDER BY language_code")
    fun observePackages(releaseId: String): Flow<List<ContentPackageEntity>>

    @Query(
        """
        SELECT * FROM content_packages
        WHERE release_id = :releaseId AND language_code = :languageCode
        """,
    )
    suspend fun getPackage(releaseId: String, languageCode: String): ContentPackageEntity?

    @Query("UPDATE content_releases SET is_active = 0 WHERE is_active = 1")
    suspend fun deactivateAllReleases()

    @Query("UPDATE content_releases SET is_active = 1 WHERE release_id = :releaseId")
    suspend fun markReleaseActive(releaseId: String): Int

    @Transaction
    suspend fun activateRelease(releaseId: String) {
        requireNotNull(getRelease(releaseId)) { "Unknown release: $releaseId" }
        deactivateAllReleases()
        check(markReleaseActive(releaseId) == 1) { "Failed to activate release: $releaseId" }
    }

    @Query("DELETE FROM content_releases WHERE release_id = :releaseId")
    suspend fun deleteRelease(releaseId: String): Int

    @Query(
        """
        SELECT q.question_id
        FROM questions AS q
        INNER JOIN study_content AS s
            ON s.release_id = q.release_id
            AND s.question_id = q.question_id
            AND s.language_code = :languageCode
        WHERE q.release_id = :releaseId
            AND (
                q.scope = 'GENERAL'
                OR (:stateCode IS NOT NULL AND q.state_code = :stateCode)
            )
        ORDER BY q.official_number, q.question_id
        """,
    )
    suspend fun getEligibleQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<String>

    @Query(
        """
        SELECT q.question_id
        FROM questions AS q
        INNER JOIN study_content AS s
            ON s.release_id = q.release_id
            AND s.question_id = q.question_id
            AND s.language_code = :languageCode
        WHERE q.release_id = :releaseId
            AND q.scope = 'GENERAL'
        ORDER BY q.official_number, q.question_id
        """,
    )
    suspend fun getGeneralQuestionIds(
        releaseId: String,
        languageCode: String,
    ): List<String>

    @Query(
        """
        SELECT q.question_id
        FROM questions AS q
        INNER JOIN study_content AS s
            ON s.release_id = q.release_id
            AND s.question_id = q.question_id
            AND s.language_code = :languageCode
        WHERE q.release_id = :releaseId
            AND q.scope = 'STATE'
            AND q.state_code = :stateCode
        ORDER BY q.official_number, q.question_id
        """,
    )
    suspend fun getStateQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String,
    ): List<String>

    @Query(
        """
        SELECT * FROM questions
        WHERE release_id = :releaseId AND question_id = :questionId
        """,
    )
    suspend fun getQuestion(releaseId: String, questionId: String): QuestionEntity?

    @Query(
        """
        SELECT * FROM answers
        WHERE release_id = :releaseId AND question_id = :questionId
        ORDER BY display_order
        """,
    )
    suspend fun getAnswers(releaseId: String, questionId: String): List<AnswerEntity>

    @Query(
        """
        SELECT * FROM study_content
        WHERE release_id = :releaseId
            AND question_id = :questionId
            AND language_code = :languageCode
        """,
    )
    suspend fun getStudyContent(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): StudyContentEntity?

    @Query(
        """
        SELECT * FROM study_answers
        WHERE release_id = :releaseId
            AND question_id = :questionId
            AND language_code = :languageCode
        ORDER BY display_order
        """,
    )
    suspend fun getStudyAnswers(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): List<StudyAnswerEntity>

    @Query(
        """
        SELECT * FROM translation_audits
        WHERE release_id = :releaseId
            AND question_id = :questionId
            AND language_code = :languageCode
        """,
    )
    suspend fun getTranslationAudit(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): TranslationAuditEntity?

    @Query(
        """
        SELECT * FROM translation_answer_audits
        WHERE release_id = :releaseId
            AND question_id = :questionId
            AND language_code = :languageCode
        ORDER BY answer_id
        """,
    )
    suspend fun getTranslationAnswerAudits(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): List<TranslationAnswerAuditEntity>

    @Query(
        """
        SELECT * FROM study_question_progress
        WHERE release_id = :releaseId
            AND language_code = :languageCode
            AND state_code = :stateCode
        ORDER BY answered_at_epoch_millis, question_id
        """,
    )
    suspend fun getStudyQuestionProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String,
    ): List<StudyQuestionProgressEntity>

    @Query(
        """
        SELECT * FROM study_question_progress
        WHERE release_id = :releaseId
            AND language_code = :languageCode
            AND state_code = :stateCode
            AND question_id = :questionId
        """,
    )
    suspend fun getStudyQuestionProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String,
        questionId: String,
    ): StudyQuestionProgressEntity?

    @Query(
        """
        SELECT
            COUNT(*) AS answered_count,
            COALESCE(SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END), 0) AS correct_count
        FROM study_question_progress
        WHERE release_id = :releaseId
            AND language_code = :languageCode
            AND state_code = :stateCode
        """,
    )
    fun observeStudyProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String,
    ): Flow<StudyProgressSummary>

    @Query(
        """
        DELETE FROM study_question_progress
        WHERE release_id = :releaseId
            AND language_code = :languageCode
            AND state_code = :stateCode
        """,
    )
    suspend fun clearStudyProgress(
        releaseId: String,
        languageCode: String,
        stateCode: String,
    )

    @Query(
        """
        SELECT * FROM mock_exam_attempts
        ORDER BY submitted_at_epoch_millis DESC, attempt_id DESC
        """,
    )
    fun observeMockExamAttempts(): Flow<List<MockExamAttemptEntity>>

    @Query("SELECT * FROM mock_exam_attempts WHERE attempt_id = :attemptId")
    suspend fun getMockExamAttempt(attemptId: String): MockExamAttemptEntity?

    @Query(
        """
        SELECT * FROM mock_exam_question_results
        WHERE attempt_id = :attemptId
        ORDER BY position
        """,
    )
    suspend fun getMockExamQuestionResults(
        attemptId: String,
    ): List<MockExamQuestionResultEntity>

    @Transaction
    suspend fun getQuestionBundle(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): LocalQuestionBundle? {
        val question = getQuestion(releaseId, questionId) ?: return null
        val studyContent = getStudyContent(releaseId, questionId, languageCode) ?: return null
        return LocalQuestionBundle(
            question = question,
            answers = getAnswers(releaseId, questionId),
            studyContent = studyContent,
            studyAnswers = getStudyAnswers(releaseId, questionId, languageCode),
            translationAudit = getTranslationAudit(releaseId, questionId, languageCode),
            translationAnswerAudits = getTranslationAnswerAudits(
                releaseId,
                questionId,
                languageCode,
            ),
        )
    }
}
