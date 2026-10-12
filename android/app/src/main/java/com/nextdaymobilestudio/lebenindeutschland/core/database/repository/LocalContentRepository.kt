package com.nextdaymobilestudio.lebenindeutschland.core.database.repository

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import kotlinx.coroutines.flow.Flow

interface LocalContentRepository {
    fun observeActiveRelease(): Flow<ContentReleaseEntity?>

    fun observeInstalledPackages(releaseId: String): Flow<List<ContentPackageEntity>>

    suspend fun replacePackage(snapshot: ContentPackageSnapshot)

    suspend fun activateRelease(releaseId: String, requiredLanguageCode: String)

    suspend fun deleteRelease(releaseId: String)

    suspend fun getEligibleQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<String>

    suspend fun getStudyQuestion(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): StudyQuestion?
}
