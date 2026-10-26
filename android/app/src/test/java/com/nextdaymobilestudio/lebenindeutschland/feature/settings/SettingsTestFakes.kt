package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.ContentPackageSnapshot
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.StudyQuestion
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal const val SETTINGS_TEST_RELEASE_ID = "lid-2026-10-05-103327-schema-3"

internal class FakeLanguagePackRepository : LocalContentRepository {
    private val activeRelease = MutableStateFlow<ContentReleaseEntity?>(null)
    private val packages = mutableMapOf<Pair<String, String>, ContentPackageEntity>()

    override fun observeActiveRelease(): Flow<ContentReleaseEntity?> = activeRelease

    override fun observeInstalledPackages(releaseId: String): Flow<List<ContentPackageEntity>> =
        MutableStateFlow(packages.values.filter { it.releaseId == releaseId })

    override suspend fun getActiveRelease(): ContentReleaseEntity? = activeRelease.value

    override suspend fun getPackage(
        releaseId: String,
        languageCode: String,
    ): ContentPackageEntity? = packages[releaseId to languageCode]

    override suspend fun replacePackage(snapshot: ContentPackageSnapshot) = Unit

    override suspend fun activateRelease(releaseId: String, requiredLanguageCode: String) = Unit

    override suspend fun deleteRelease(releaseId: String) = Unit

    override suspend fun getEligibleQuestionIds(
        releaseId: String,
        languageCode: String,
        stateCode: String?,
    ): List<String> = emptyList()

    override suspend fun getStudyQuestion(
        releaseId: String,
        questionId: String,
        languageCode: String,
    ): StudyQuestion? = null

    fun seedInstalledLanguages(vararg languageCodes: String) {
        activeRelease.value = releaseEntity()
        languageCodes.forEach { languageCode ->
            packages[SETTINGS_TEST_RELEASE_ID to languageCode] = packageEntity(languageCode)
        }
    }
}

private fun releaseEntity(): ContentReleaseEntity = ContentReleaseEntity(
    releaseId = SETTINGS_TEST_RELEASE_ID,
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

private fun packageEntity(languageCode: String): ContentPackageEntity = ContentPackageEntity(
    releaseId = SETTINGS_TEST_RELEASE_ID,
    languageCode = languageCode,
    schemaVersion = 3,
    packageStrategy = "SELF_CONTAINED_LANGUAGE_PACK",
    status = "READY",
    createdAt = "2026-10-12T00:00:00Z",
    packageSha256 = "c".repeat(64),
    questionCount = 460,
    chunkCount = 46,
    chunkSize = 10,
    jsonBytes = 1_000L,
    qualityIssueCount = 0,
    explanationReady = true,
    installedAtEpochMillis = 123L,
)
