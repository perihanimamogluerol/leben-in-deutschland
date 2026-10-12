package com.nextdaymobilestudio.lebenindeutschland.core.content.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalJsonTest {
    @Test
    fun objectsAreSortedAndUnicodeIsPreserved() {
        val value = linkedMapOf<String, Any?>(
            "z" to listOf(true, null, "İfade özgürlüğü"),
            "a" to mapOf("b" to 2L, "a" to 1L),
        )

        assertEquals(
            "{\"a\":{\"a\":1,\"b\":2},\"z\":[true,null,\"İfade özgürlüğü\"]}",
            CanonicalJson.encode(value),
        )
    }

    @Test
    fun sha256MatchesPythonPipelineFixture() {
        assertEquals(
            "43258cff783fe7036d8a43033f830adfc60ec037382473548ac742b888292777",
            CanonicalJson.sha256(mapOf("b" to 2L, "a" to 1L)),
        )
    }
}
