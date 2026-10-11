package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection

import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState
import org.junit.Assert.assertEquals
import org.junit.Test

class GermanStateTest {
    @Test
    fun `catalog contains the 16 German federal states`() {
        assertEquals(16, GermanState.entries.size)
    }

    @Test
    fun `state codes and German names are unique`() {
        assertEquals(
            GermanState.entries.size,
            GermanState.entries.map(GermanState::code).toSet().size,
        )
        assertEquals(
            GermanState.entries.size,
            GermanState.entries.map(GermanState::germanName).toSet().size,
        )
    }

    @Test
    fun `state codes match the content catalog prefixes`() {
        assertEquals(
            setOf(
                "bw", "by", "be", "bb", "hb", "hh", "he", "mv",
                "ni", "nw", "rp", "sl", "sn", "st", "sh", "th",
            ),
            GermanState.entries.map(GermanState::code).toSet(),
        )
    }
}
