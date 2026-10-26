package com.nextdaymobilestudio.lebenindeutschland.feature.settings

import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSyncResult
import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSynchronizer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LanguageChangeCoordinatorTest {
    @Test
    fun `language that is already installed switches without any network access`() = runBlocking {
        val local = FakeLanguagePackRepository().apply { seedInstalledLanguages("de", "tr") }
        var synchronizerCreated = false
        val coordinator = LanguageChangeCoordinator(
            localContentRepository = local,
            contentSynchronizerProvider = {
                synchronizerCreated = true
                ContentSynchronizer { error("must not be called") }
            },
        )

        assertEquals(LanguageChangeResult.Ready, coordinator.prepare("tr"))
        assertEquals(false, synchronizerCreated)
    }

    @Test
    fun `missing language package is downloaded before the change is ready`() = runBlocking {
        val local = FakeLanguagePackRepository().apply { seedInstalledLanguages("de", "tr") }
        val requestedLanguages = mutableListOf<String>()
        val coordinator = LanguageChangeCoordinator(
            localContentRepository = local,
            contentSynchronizerProvider = {
                ContentSynchronizer { languageCode ->
                    requestedLanguages += languageCode
                    local.seedInstalledLanguages("de", "tr", languageCode)
                    syncResult(languageCode)
                }
            },
        )

        assertEquals(LanguageChangeResult.Ready, coordinator.prepare("ar"))
        assertEquals(listOf("ar"), requestedLanguages)
    }

    @Test
    fun `first language without any local release is downloaded`() = runBlocking {
        val requestedLanguages = mutableListOf<String>()
        val coordinator = LanguageChangeCoordinator(
            localContentRepository = FakeLanguagePackRepository(),
            contentSynchronizerProvider = {
                ContentSynchronizer { languageCode ->
                    requestedLanguages += languageCode
                    syncResult(languageCode)
                }
            },
        )

        assertEquals(LanguageChangeResult.Ready, coordinator.prepare("en"))
        assertEquals(listOf("en"), requestedLanguages)
    }

    @Test
    fun `failed download is reported and does not become ready`() = runBlocking {
        val expectedException = IllegalStateException("offline")
        var reportedMessage: String? = null
        var reportedThrowable: Throwable? = null
        val coordinator = LanguageChangeCoordinator(
            localContentRepository = FakeLanguagePackRepository().apply {
                seedInstalledLanguages("de", "tr")
            },
            contentSynchronizerProvider = {
                ContentSynchronizer { throw expectedException }
            },
            reportError = { message, throwable ->
                reportedMessage = message
                reportedThrowable = throwable
            },
        )

        assertEquals(LanguageChangeResult.Failed, coordinator.prepare("ar"))
        assertEquals("Language change failed for language ar", reportedMessage)
        assertSame(expectedException, reportedThrowable)
    }

    @Test
    fun `failure to create the synchronizer is treated as a failed change`() = runBlocking {
        val coordinator = LanguageChangeCoordinator(
            localContentRepository = FakeLanguagePackRepository(),
            contentSynchronizerProvider = { error("Firebase is not configured") },
        )

        assertEquals(LanguageChangeResult.Failed, coordinator.prepare("fr"))
    }

    @Test
    fun `cancellation is propagated instead of being reported as a failure`() = runBlocking {
        var reported = false
        val coordinator = LanguageChangeCoordinator(
            localContentRepository = FakeLanguagePackRepository(),
            contentSynchronizerProvider = {
                ContentSynchronizer { throw CancellationException("cancelled") }
            },
            reportError = { _, _ -> reported = true },
        )

        try {
            coordinator.prepare("fr")
            fail("Expected cancellation to propagate")
        } catch (exception: CancellationException) {
            assertTrue(exception.message == "cancelled")
        }
        assertEquals(false, reported)
    }

    private fun syncResult(languageCode: String) = ContentSyncResult(
        releaseId = SETTINGS_TEST_RELEASE_ID,
        selectedLanguageCode = languageCode,
        installedLanguageCodes = listOf("de", languageCode).distinct(),
        questionCount = 460,
        activated = true,
    )
}
