package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model

import java.util.Locale

enum class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val germanName: String,
    val isRightToLeft: Boolean = false,
) {
    GERMAN(
        code = "de",
        nativeName = "Deutsch",
        germanName = "Deutsch",
    ),
    ENGLISH(
        code = "en",
        nativeName = "English",
        germanName = "Englisch",
    ),
    TURKISH(
        code = "tr",
        nativeName = "Türkçe",
        germanName = "Türkisch",
    ),
    UKRAINIAN(
        code = "uk",
        nativeName = "Українська",
        germanName = "Ukrainisch",
    ),
    ARABIC(
        code = "ar",
        nativeName = "العربية",
        germanName = "Arabisch",
        isRightToLeft = true,
    ),
    ROMANIAN(
        code = "ro",
        nativeName = "Română",
        germanName = "Rumänisch",
    ),
    POLISH(
        code = "pl",
        nativeName = "Polski",
        germanName = "Polnisch",
    ),
    RUSSIAN(
        code = "ru",
        nativeName = "Русский",
        germanName = "Russisch",
    ),
    BULGARIAN(
        code = "bg",
        nativeName = "Български",
        germanName = "Bulgarisch",
    ),
    CROATIAN(
        code = "hr",
        nativeName = "Hrvatski",
        germanName = "Kroatisch",
    ),
    BOSNIAN(
        code = "bs",
        nativeName = "Bosanski",
        germanName = "Bosnisch",
    ),
    SERBIAN_LATIN(
        code = "sr-Latn",
        nativeName = "Srpski",
        germanName = "Serbisch (Lateinisch)",
    ),
    GREEK(
        code = "el",
        nativeName = "Ελληνικά",
        germanName = "Griechisch",
    ),
    ALBANIAN(
        code = "sq",
        nativeName = "Shqip",
        germanName = "Albanisch",
    ),
    PERSIAN_DARI(
        code = "fa",
        nativeName = "فارسی / دری",
        germanName = "Persisch / Dari",
        isRightToLeft = true,
    ),
    PASHTO(
        code = "ps",
        nativeName = "پښتو",
        germanName = "Paschtu",
        isRightToLeft = true,
    ),
    KURDISH_KURMANJI(
        code = "ku",
        nativeName = "Kurmancî",
        germanName = "Kurdisch (Kurmancî)",
    ),
    ITALIAN(
        code = "it",
        nativeName = "Italiano",
        germanName = "Italienisch",
    ),
    SPANISH(
        code = "es",
        nativeName = "Español",
        germanName = "Spanisch",
    ),
    HUNGARIAN(
        code = "hu",
        nativeName = "Magyar",
        germanName = "Ungarisch",
    ),
    FRENCH(
        code = "fr",
        nativeName = "Français",
        germanName = "Französisch",
    ),
    ;

    companion object {
        fun fromCode(code: String?): SupportedLanguage? =
            entries.firstOrNull { language -> language.code == code }

        fun fromLanguageTag(languageTag: String): SupportedLanguage =
            findByLanguageTag(languageTag) ?: GERMAN

        fun fromLanguageTags(languageTags: String): SupportedLanguage =
            languageTags
                .split(',')
                .firstNotNullOfOrNull(::findByLanguageTag)
                ?: GERMAN

        private fun findByLanguageTag(languageTag: String): SupportedLanguage? {
            val requestedLanguage = Locale.forLanguageTag(languageTag).language

            return entries.firstOrNull { supportedLanguage ->
                Locale.forLanguageTag(supportedLanguage.code).language == requestedLanguage
            }
        }
    }
}
