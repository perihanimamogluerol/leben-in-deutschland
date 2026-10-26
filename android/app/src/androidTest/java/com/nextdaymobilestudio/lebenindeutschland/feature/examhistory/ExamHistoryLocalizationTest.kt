package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory

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
class ExamHistoryLocalizationTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySupportedLanguageHasCompleteLocalizedExamHistoryContent() {
        val germanContext = targetContext.withLocale("de")

        SupportedLanguage.entries.forEach { language ->
            val localizedContext = targetContext.withLocale(language.code)
            EXAM_HISTORY_STRINGS.forEach { resource ->
                val localized = localizedContext.localizedString(resource)
                assertTrue("${language.code} has a blank string for $resource", localized.isNotBlank())
                if (language != SupportedLanguage.GERMAN) {
                    assertNotEquals(
                        "${language.code} falls back to German for $resource",
                        germanContext.localizedString(resource),
                        localized,
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

    private fun Context.localizedString(resource: Int): String = when (resource) {
        R.string.exam_history_duration -> getString(resource, 42)
        R.string.exam_history_your_answer,
        R.string.exam_history_correct_answer -> getString(resource, "A")
        else -> getString(resource)
    }

    companion object {
        private val EXAM_HISTORY_STRINGS = intArrayOf(
            R.string.home_exam_history,
            R.string.exam_history_title,
            R.string.exam_history_empty_title,
            R.string.exam_history_empty_description,
            R.string.exam_history_view_result,
            R.string.exam_history_detail_title,
            R.string.exam_history_duration,
            R.string.exam_history_correct,
            R.string.exam_history_wrong,
            R.string.exam_history_unanswered,
            R.string.exam_history_your_answer,
            R.string.exam_history_correct_answer,
            R.string.exam_history_content_unavailable,
            R.string.exam_history_error,
        )
    }
}
