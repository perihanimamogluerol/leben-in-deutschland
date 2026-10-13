package com.nextdaymobilestudio.lebenindeutschland.feature.home

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import java.util.Locale
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeLocalizationTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySupportedLanguageHasCompleteLocalizedHomeContent() {
        val germanContext = targetContext.withLocale("de")

        SupportedLanguage.entries.forEach { language ->
            val localizedContext = targetContext.withLocale(language.code)

            HOME_STRING_RESOURCES.forEach { stringResource ->
                val localizedText = localizedContext.localizedString(stringResource)
                assertTrue(
                    "${language.code} has a blank string for resource $stringResource",
                    localizedText.isNotBlank(),
                )
                if (language != SupportedLanguage.GERMAN) {
                    assertNotEquals(
                        "${language.code} falls back to German for resource $stringResource",
                        germanContext.localizedString(stringResource),
                        localizedText,
                    )
                }
            }
        }
    }

    private fun Context.withLocale(languageTag: String): Context {
        val configuration = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return createConfigurationContext(configuration)
    }

    private fun Context.localizedString(stringResource: Int): String =
        if (stringResource == R.string.home_questions_available) {
            getString(stringResource, 460)
        } else {
            getString(stringResource)
        }

    companion object {
        private val HOME_STRING_RESOURCES = intArrayOf(
            R.string.home_title,
            R.string.home_description,
            R.string.home_your_selection,
            R.string.home_content_ready,
            R.string.home_questions_available,
            R.string.home_language,
            R.string.home_federal_state,
            R.string.home_start_studying,
            R.string.home_start_hint,
        )
    }
}
