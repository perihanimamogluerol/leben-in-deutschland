package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageSelectionLocalizationTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySupportedLanguageHasCompleteLocalizedScreenContentAndDirection() {
        val germanContext = targetContext.withLocale("de")

        SupportedLanguage.entries.forEach { language ->
            val localizedContext = targetContext.withLocale(language.code)

            SCREEN_STRING_RESOURCES.forEach { stringResource ->
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

            val expectedDirection = if (language.isRightToLeft) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
            assertEquals(
                "Unexpected layout direction for ${language.code}",
                expectedDirection,
                localizedContext.resources.configuration.layoutDirection,
            )
        }
    }

    private fun Context.withLocale(languageTag: String): Context {
        val configuration = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return createConfigurationContext(configuration)
    }

    private fun Context.localizedString(stringResource: Int): String =
        if (stringResource == R.string.language_selection_temporary_confirmation) {
            getString(stringResource, "Deutsch")
        } else {
            getString(stringResource)
        }

    companion object {
        private val SCREEN_STRING_RESOURCES = intArrayOf(
            R.string.language_selection_title,
            R.string.language_selection_description,
            R.string.language_selection_search_label,
            R.string.language_selection_search_placeholder,
            R.string.language_selection_no_results_title,
            R.string.language_selection_no_results_message,
            R.string.language_selection_change_later,
            R.string.language_selection_continue,
            R.string.language_selection_temporary_confirmation,
        )
    }
}
