package com.nextdaymobilestudio.lebenindeutschland.core.database.repository

import androidx.room.withTransaction
import com.nextdaymobilestudio.lebenindeutschland.core.database.LocalContentDatabase
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.toStudyQuestion
import kotlinx.coroutines.flow.Flow

class RoomLocalContentRepository(
    private val database: LocalContentDatabase,
) : LocalContentRepository {
    private val contentDao = database.contentDao()

    override fun observeActiveRelease(): Flow<ContentReleaseEntity?> =
        contentDao.observeActiveRelease()

    override fun observeInstalledPackages(releaseId: String): Flow<List<ContentPackageEntity>> =
        contentDao.observePackages(releaseId)

    override suspend fun getActiveRelease(): ContentReleaseEntity? =
        contentDao.getActiveRelease()

    override suspend fun getPackage(
        releaseId: String,
        languageCode: String,
    ): ContentPackageEntity? = contentDao.getPackage(releaseId, languageCode)

    override suspend fun replacePackage(snapshot: ContentPackageSnapshot) {
        snapshot.requireValid()
        database.withTransaction {
            val existingRelease = contentDao.getRelease(snapshot.release.releaseId)
            require(
                existingRelease == null ||
                    existingRelease.catalogSha256 == snapshot.release.catalogSha256,
            ) { "A release ID cannot be reused with a different catalog hash" }

            contentDao.upsertRelease(
                snapshot.release.copy(isActive = existingRelease?.isActive ?: false),
            )
            contentDao.upsertReleaseLanguages(snapshot.releaseLanguages)
            contentDao.deleteStudyContent(
                snapshot.packageMetadata.releaseId,
                snapshot.packageMetadata.languageCode,
            )
            contentDao.deletePackageMetadata(
                snapshot.packageMetadata.releaseId,
                snapshot.packageMetadata.languageCode,
            )
            contentDao.upsertQuestions(snapshot.questions)
            contentDao.upsertAnswers(snapshot.answers)
            contentDao.upsertStudyContent(snapshot.studyContent)
            contentDao.upsertStudyAnswers(snapshot.studyAnswers)
            if (snapshot.translationAudits.isNotEmpty()) {
                contentDao.upsertTranslationAudits(snapshot.translationAudits)
            }
            if (snapshot.translationAnswerAudits.isNotEmpty()) {
                contentDao.upsertTranslationAnswerAudits(snapshot.translationAnswerAudits)
            }
            contentDao.upsertPackage(snapshot.packageMetadata)
        }
    }

    override suspend fun activateRelease(releaseId: String, requiredLanguageCode: String) {
        database.withTransaction {
            requireNotNull(contentDao.getPackage(releaseId, GERMAN_LANGUAGE_CODE)) {
                "German package must be installed before activation"
            }
            requireNotNull(contentDao.getPackage(releaseId, requiredLanguageCode)) {
                "$requiredLanguageCode package must be installed before activation"
            }
            contentDao.activateRelease(releaseId)
        }
    }

    override suspend fun deleteRelease(releaseId: String) {
        database.withTransaction {
            val release = contentDao.getRelease(releaseId) ?: return@withTransaction
            require(!release.isActive) { "The active release cannot be deleted" }
            check(contentDao.deleteRelease(releaseId) == 1) {
                "Failed to delete release: $releaseId"
            }
        }
    }

    override suspend fun getEligibleQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<String> = contentDao.getEligibleQuestionIds(releaseId, languageCode, stateCode)

    override suspend fun getStudyQuestion(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): StudyQuestion? = contentDao.getQuestionBundle(releaseId, questionId, languageCode)
        ?.toStudyQuestion()

    private companion object {
        const val GERMAN_LANGUAGE_CODE = "de"
    }
}
