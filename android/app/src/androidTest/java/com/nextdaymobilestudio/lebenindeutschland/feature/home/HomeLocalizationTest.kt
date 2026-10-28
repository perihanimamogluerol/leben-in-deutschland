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

    private fun Context.localizedString(stringResource: Int): String = when (stringResource) {
        R.string.home_questions_available -> getString(stringResource, 460)
        R.string.home_progress_answered -> getString(stringResource, 12, 310)
        R.string.home_progress_correct -> getString(stringResource, 9, 3)
        R.string.home_wrong_answers_count -> getString(stringResource, 3)
        else -> getString(stringResource)
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
            R.string.home_continue_studying,
            R.string.home_study_again,
            R.string.home_progress_title,
            R.string.home_progress_answered,
            R.string.home_progress_correct,
            R.string.home_start_hint,
            R.string.home_wrong_answers_title,
            R.string.home_wrong_answers_count,
            R.string.home_wrong_answers_empty,
            R.string.home_review_wrong_answers,
        )
    }
}
