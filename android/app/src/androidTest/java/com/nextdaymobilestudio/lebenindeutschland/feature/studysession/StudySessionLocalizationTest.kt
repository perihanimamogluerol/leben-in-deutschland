package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

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
class StudySessionLocalizationTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySupportedLanguageHasCompleteLocalizedStudyContent() {
        val germanContext = targetContext.withLocale("de")

        SupportedLanguage.entries.forEach { language ->
            val localizedContext = targetContext.withLocale(language.code)

            STUDY_STRING_RESOURCES.forEach { stringResource ->
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
        R.string.study_progress -> getString(stringResource, 1, 460)
        R.string.study_answer_image_description -> getString(stringResource, "A")
        R.string.study_complete_description -> getString(stringResource, 320, 460)
        R.string.study_wrong_answers_complete_description -> getString(stringResource, 3)
        else -> getString(stringResource)
    }

    companion object {
        private val STUDY_STRING_RESOURCES = intArrayOf(
            R.string.study_title,
            R.string.study_progress,
            R.string.study_question_image_description,
            R.string.study_answer_image_description,
            R.string.study_correct,
            R.string.study_incorrect,
            R.string.study_explanation,
            R.string.study_memory_key_title,
            R.string.study_check_answer,
            R.string.study_next_question,
            R.string.study_finish,
            R.string.study_error_title,
            R.string.study_error_description,
            R.string.study_complete_title,
            R.string.study_complete_description,
            R.string.study_back_home,
            R.string.study_wrong_answers_title,
            R.string.study_wrong_answers_empty_title,
            R.string.study_wrong_answers_empty_description,
            R.string.study_wrong_answers_complete_title,
            R.string.study_wrong_answers_complete_description,
        )
    }
}
