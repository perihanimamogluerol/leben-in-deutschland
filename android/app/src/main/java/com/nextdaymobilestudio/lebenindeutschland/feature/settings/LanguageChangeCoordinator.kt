package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSynchronizer
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import kotlin.coroutines.cancellation.CancellationException

enum class LanguageChangeResult {
    Ready,
    Failed,
}

/**
 * Makes sure the content package for a newly selected language is usable on the device
 * before the application switches to it.
 *
 * The coordinator never changes the persisted language itself. Callers switch only after
 * [prepare] returns [LanguageChangeResult.Ready], so a failed or cancelled download leaves
 * the current language and its content fully active.
 */
class LanguageChangeCoordinator(
    private val localContentRepository: LocalContentRepository,
    private val contentSynchronizerProvider: () -> ContentSynchronizer,
    private val reportError: (String, Throwable) -> Unit = { _, _ -> },
) {
    suspend fun prepare(languageCode: String): LanguageChangeResult {
        if (hasLocalPackage(languageCode)) {
            return LanguageChangeResult.Ready
        }

        return try {
            contentSynchronizerProvider().synchronize(languageCode)
            LanguageChangeResult.Ready
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reportError("Language change failed for language $languageCode", exception)
            LanguageChangeResult.Failed
        }
    }

    private suspend fun hasLocalPackage(languageCode: String): Boolean {
        val release = localContentRepository.getActiveRelease() ?: return false
        return setOf(GERMAN_LANGUAGE_CODE, languageCode).all { requiredLanguageCode ->
            localContentRepository.getPackage(release.releaseId, requiredLanguageCode) != null
        }
    }

    private companion object {
        const val GERMAN_LANGUAGE_CODE = "de"
    }
}
