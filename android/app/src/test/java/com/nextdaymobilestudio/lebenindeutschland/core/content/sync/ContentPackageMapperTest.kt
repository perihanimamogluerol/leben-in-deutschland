package com.nextdaymobilestudio.lebenindeutschland.core.content.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentPackageMapperTest {
    private val mapper = ContentPackageMapper()

    @Test
    fun verifiedFirestoreDocumentsMapToNormalizedSnapshot() {
        val fixture = remoteContentFixture()
        val release = mapper.validateRelease(fixture.manifest, fixture.release, 123L)

        val snapshot = mapper.mapPackage(
            release = release,
            packageDocument = fixture.packages.getValue("en"),
            chunkDocuments = fixture.chunks.getValue("en"),
            installedAtEpochMillis = 123L,
        )

        assertEquals(RELEASE_ID, snapshot.release.releaseId)
        assertEquals("en", snapshot.packageMetadata.languageCode)
        assertEquals(1, snapshot.questions.size)
        assertEquals(4, snapshot.answers.size)
        assertEquals("image/jpeg", snapshot.questions.single().questionImageMimeType)
        assertEquals("images/answer-D.png", snapshot.answers.last().imageSourcePath)
        assertEquals("source-record-id", snapshot.translationAudits.single().sourceRecordId)
        assertEquals(4, snapshot.translationAnswerAudits.size)
        assertEquals(true, snapshot.translationAnswerAudits[1].usedFallback)
        assertEquals("Frage erkennen", snapshot.questions.single().memoryQuestionCue)
        assertEquals("Antwort A", snapshot.questions.single().memoryAnswerCue)
        assertEquals(
            "AI_GENERATED_NEEDS_REVIEW",
            snapshot.questions.single().memoryKeyStatus,
        )
    }

    @Test
    fun tamperedChunkIsRejectedBeforeMapping() {
        val fixture = remoteContentFixture()
        val release = mapper.validateRelease(fixture.manifest, fixture.release, 123L)
        val chunk = fixture.chunks.getValue("en").single()
        val tampered = chunk.copy(
            fields = chunk.fields + ("firstQuestionId" to "tampered"),
        )

        assertThrows(ContentSyncException::class.java) {
            mapper.mapPackage(
                release = release,
                packageDocument = fixture.packages.getValue("en"),
                chunkDocuments = listOf(tampered),
                installedAtEpochMillis = 123L,
            )
        }
    }

    @Test
    fun manifestPointingToAnotherReleaseIsRejected() {
        val fixture = remoteContentFixture()
        val invalidManifest = fixture.manifest.copy(
            fields = fixture.manifest.fields + ("activeReleaseId" to "other-release"),
        )

        assertThrows(ContentSyncException::class.java) {
            mapper.validateRelease(invalidManifest, fixture.release, 123L)
        }
    }

    @Test
    fun manifestWithDifferentDatasetVersionIsRejected() {
        val fixture = remoteContentFixture()
        val invalidManifest = fixture.manifest.copy(
            fields = fixture.manifest.fields + ("datasetVersion" to "other-dataset"),
        )

        assertThrows(ContentSyncException::class.java) {
            mapper.validateRelease(invalidManifest, fixture.release, 123L)
        }
    }
}
