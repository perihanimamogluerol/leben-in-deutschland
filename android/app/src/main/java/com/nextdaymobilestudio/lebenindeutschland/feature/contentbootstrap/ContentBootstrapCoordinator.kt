package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap

import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSynchronizer
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ContentBootstrapCoordinator(
    private val localContentRepository: LocalContentRepository,
    private val contentSynchronizerProvider: () -> ContentSynchronizer,
    private val reportError: (String, Throwable) -> Unit = { _, _ -> },
) {
    fun bootstrap(selectedLanguageCode: String): Flow<ContentBootstrapUiState> = flow {
        val localRelease = findUsableLocalRelease(selectedLanguageCode)
        if (localRelease == null) {
            emit(ContentBootstrapUiState.Loading)
        } else {
            emit(localRelease.toReadyState(isRefreshing = true))
        }

        try {
            val result = contentSynchronizerProvider().synchronize(selectedLanguageCode)
            emit(
                ContentBootstrapUiState.Ready(
                    releaseId = result.releaseId,
                    questionCount = result.questionCount,
                    isRefreshing = false,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reportError(
                "Content bootstrap failed for language $selectedLanguageCode",
                exception,
            )
            if (localRelease == null) {
                emit(ContentBootstrapUiState.Error)
            } else {
                emit(
                    localRelease.toReadyState(
                        isRefreshing = false,
                        refreshFailed = true,
                    ),
                )
            }
        }
    }

    private suspend fun findUsableLocalRelease(
        selectedLanguageCode: String,
    ): ContentReleaseEntity? {
        val release = localContentRepository.getActiveRelease() ?: return null
        val requiredLanguages = setOf(GERMAN_LANGUAGE_CODE, selectedLanguageCode)
        val hasEveryRequiredPackage = requiredLanguages.all { languageCode ->
            localContentRepository.getPackage(release.releaseId, languageCode) != null
        }
        return release.takeIf { hasEveryRequiredPackage }
    }

    private fun ContentReleaseEntity.toReadyState(
        isRefreshing: Boolean,
        refreshFailed: Boolean = false,
    ): ContentBootstrapUiState.Ready = ContentBootstrapUiState.Ready(
        releaseId = releaseId,
        questionCount = questionCount,
        isRefreshing = isRefreshing,
        refreshFailed = refreshFailed,
    )

    private companion object {
        const val GERMAN_LANGUAGE_CODE = "de"
    }
}
