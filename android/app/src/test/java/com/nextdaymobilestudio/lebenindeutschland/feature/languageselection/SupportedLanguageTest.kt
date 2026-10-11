package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupportedLanguageTest {
    @Test
    fun `supported language codes are unique`() {
        val codes = SupportedLanguage.entries.map(SupportedLanguage::code)

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `only Arabic Persian Dari and Pashto use right to left layout`() {
        val rightToLeftLanguages = SupportedLanguage.entries
            .filter(SupportedLanguage::isRightToLeft)
            .map(SupportedLanguage::code)
            .toSet()

        assertEquals(setOf("ar", "fa", "ps"), rightToLeftLanguages)
    }

    @Test
    fun `regional device locale resolves to supported language`() {
        assertEquals(
            SupportedLanguage.TURKISH,
            SupportedLanguage.fromLanguageTag("tr-TR"),
        )
        assertEquals(
            SupportedLanguage.ENGLISH,
            SupportedLanguage.fromLanguageTag("en-GB"),
        )
    }

    @Test
    fun `any Serbian device locale resolves to supported Latin Serbian`() {
        assertEquals(
            SupportedLanguage.SERBIAN_LATIN,
            SupportedLanguage.fromLanguageTag("sr-Cyrl-RS"),
        )
    }

    @Test
    fun `unsupported or empty device locale falls back to German`() {
        assertEquals(
            SupportedLanguage.GERMAN,
            SupportedLanguage.fromLanguageTag("ja-JP"),
        )
        assertEquals(
            SupportedLanguage.GERMAN,
            SupportedLanguage.fromLanguageTag(""),
        )
    }

    @Test
    fun `first supported locale is selected from device locale list`() {
        assertEquals(
            SupportedLanguage.TURKISH,
            SupportedLanguage.fromLanguageTags("ja-JP,tr-TR,en-GB"),
        )
    }

    @Test
    fun `stored language code resolves only supported languages`() {
        assertEquals(
            SupportedLanguage.SERBIAN_LATIN,
            SupportedLanguage.fromCode("sr-Latn"),
        )
        assertNull(SupportedLanguage.fromCode("ja"))
        assertNull(SupportedLanguage.fromCode(null))
    }
}
