package com.nextdaymobilestudio.lebenindeutschland.core.content.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirebaseContentSynchronizerFactoryTest {
    @Test
    fun expectedProjectIsAccepted() {
        assertEquals(
            FirebaseContentSynchronizerFactory.EXPECTED_PROJECT_ID,
            FirebaseContentSynchronizerFactory.requireExpectedFirebaseProject(
                FirebaseContentSynchronizerFactory.EXPECTED_PROJECT_ID,
            ),
        )
    }

    @Test
    fun missingOrDifferentProjectIsRejected() {
        assertThrows(ContentSyncException::class.java) {
            FirebaseContentSynchronizerFactory.requireExpectedFirebaseProject(null)
        }
        assertThrows(ContentSyncException::class.java) {
            FirebaseContentSynchronizerFactory.requireExpectedFirebaseProject(
                "another-project",
            )
        }
    }
}
