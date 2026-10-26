package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam

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
class MockExamLocalizationTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySupportedLanguageHasCompleteLocalizedMockExamContent() {
        val germanContext = targetContext.withLocale("de")

        SupportedLanguage.entries.forEach { language ->
            val localizedContext = targetContext.withLocale(language.code)
            MOCK_EXAM_STRING_RESOURCES.forEach { stringResource ->
                val localizedText = localizedContext.localizedString(stringResource)
                assertTrue(
                    "${language.code} has a blank mock-exam string for $stringResource",
                    localizedText.isNotBlank(),
                )
                if (language != SupportedLanguage.GERMAN) {
                    assertNotEquals(
                        "${language.code} falls back to German for $stringResource",
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
        R.string.mock_exam_progress,
        R.string.mock_exam_answered,
        R.string.mock_exam_result_score -> getString(stringResource, 7, 33)
        R.string.mock_exam_confirm_description -> getString(stringResource, 7, 26)
        R.string.mock_exam_result_breakdown -> getString(stringResource, 17, 10, 6)
        R.string.mock_exam_result_threshold -> getString(stringResource, 17)
        else -> getString(stringResource)
    }

    companion object {
        private val MOCK_EXAM_STRING_RESOURCES = intArrayOf(
            R.string.home_mock_exam_title,
            R.string.home_mock_exam_description,
            R.string.home_mock_exam_start,
            R.string.home_mock_exam_resume,
            R.string.home_mock_exam_result,
            R.string.mock_exam_title,
            R.string.mock_exam_setup_title,
            R.string.mock_exam_setup_description,
            R.string.mock_exam_lid_title,
            R.string.mock_exam_lid_description,
            R.string.mock_exam_naturalization_title,
            R.string.mock_exam_naturalization_description,
            R.string.mock_exam_start,
            R.string.mock_exam_progress,
            R.string.mock_exam_answered,
            R.string.mock_exam_previous,
            R.string.mock_exam_next,
            R.string.mock_exam_finish,
            R.string.mock_exam_confirm_title,
            R.string.mock_exam_confirm_description,
            R.string.mock_exam_cancel,
            R.string.mock_exam_submit,
            R.string.mock_exam_error_title,
            R.string.mock_exam_error_description,
            R.string.mock_exam_retry,
            R.string.mock_exam_passed_title,
            R.string.mock_exam_failed_title,
            R.string.mock_exam_result_score,
            R.string.mock_exam_result_breakdown,
            R.string.mock_exam_result_threshold,
            R.string.mock_exam_time_up,
            R.string.mock_exam_new,
            R.string.mock_exam_back_home,
        )
    }
}
