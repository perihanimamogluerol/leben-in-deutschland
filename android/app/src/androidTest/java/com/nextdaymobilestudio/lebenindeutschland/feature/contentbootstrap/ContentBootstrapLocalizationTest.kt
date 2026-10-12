package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap

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
class ContentBootstrapLocalizationTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySupportedLanguageHasCompleteLocalizedContent() {
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
        }
    }

    private fun Context.withLocale(languageTag: String): Context {
        val configuration = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return createConfigurationContext(configuration)
    }

    private fun Context.localizedString(stringResource: Int): String =
        if (stringResource == R.string.content_bootstrap_ready_description) {
            getString(stringResource, 460)
        } else {
            getString(stringResource)
        }

    companion object {
        private val SCREEN_STRING_RESOURCES = intArrayOf(
            R.string.content_bootstrap_loading_title,
            R.string.content_bootstrap_loading_description,
            R.string.content_bootstrap_ready_title,
            R.string.content_bootstrap_ready_description,
            R.string.content_bootstrap_refreshing,
            R.string.content_bootstrap_refresh_failed,
            R.string.content_bootstrap_error_title,
            R.string.content_bootstrap_error_description,
            R.string.content_bootstrap_retry,
        )
    }
}
