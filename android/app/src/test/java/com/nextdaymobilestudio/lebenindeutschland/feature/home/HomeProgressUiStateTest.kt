package com.nextdaymobilestudio.lebenindeutschland.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeProgressUiStateTest {
    @Test
    fun progressCalculatesFractionAndCompletion() {
        val partial = HomeProgressUiState(
            answeredCount = 155,
            correctCount = 120,
            totalCount = 310,
        )
        val complete = partial.copy(answeredCount = 310)

        assertEquals(0.5f, partial.fraction)
        assertFalse(partial.isComplete)
        assertTrue(complete.isComplete)
    }

    @Test
    fun emptyProgressIsSafe() {
        val progress = HomeProgressUiState()

        assertEquals(0f, progress.fraction)
        assertFalse(progress.isComplete)
    }
}
