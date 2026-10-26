package com.nextdaymobilestudio.lebenindeutschland.core.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nextdaymobilestudio.lebenindeutschland.LebenInDeutschlandApplication
import com.nextdaymobilestudio.lebenindeutschland.core.database.LocalContentDatabase
import com.nextdaymobilestudio.lebenindeutschland.feature.studysession.StudySessionCoordinator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the real Schema 3 package downloaded by the application.
 *
 * This is intentionally a device test: run the application through content bootstrap first,
 * then execute only this class against the persistent production Room database. A regular
 * connected test run skips this check when that external prerequisite is not available.
 */
@RunWith(AndroidJUnit4::class)
class ProductionContentIntegrityTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun installedProductionCatalogContainsAll460UsableQuestions() = runBlocking {
        val application = targetContext.applicationContext as LebenInDeutschlandApplication
        val repository = application.appContainer.localContentRepository
        val activeRelease = repository.getActiveRelease()
        assumeTrue(
            "No active content release. Open the app and complete content preparation first.",
            activeRelease != null,
        )
        val release = requireNotNull(activeRelease)
        assertEquals(3, release.schemaVersion)
        assertEquals(460, release.questionCount)

        val databasePath = targetContext.getDatabasePath(LocalContentDatabase.DATABASE_NAME)
        assertTrue(
            "Content database is missing. Open the app and complete content preparation first.",
            databasePath.exists(),
        )

        val database = SQLiteDatabase.openDatabase(
            databasePath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        val (questionIds, installedLanguages) = database.use { db ->
            val releaseArgument = arrayOf(release.releaseId)

            assertEquals(
                460L,
                db.count(
                    "SELECT COUNT(*) FROM questions WHERE release_id = ?",
                    releaseArgument,
                ),
            )
            assertEquals(
                300L,
                db.count(
                    """
                    SELECT COUNT(*) FROM questions
                    WHERE release_id = ? AND scope = 'GENERAL'
                    """.trimIndent(),
                    releaseArgument,
                ),
            )
            assertEquals(
                160L,
                db.count(
                    """
                    SELECT COUNT(*) FROM questions
                    WHERE release_id = ? AND scope = 'STATE'
                    """.trimIndent(),
                    releaseArgument,
                ),
            )
            assertEquals(expectedStateQuestionCounts(), db.stateQuestionCounts(release.releaseId))
            assertEquals(
                1_840L,
                db.count(
                    "SELECT COUNT(*) FROM answers WHERE release_id = ?",
                    releaseArgument,
                ),
            )
            assertEquals(
                0L,
                db.count(
                    """
                    SELECT COUNT(*) FROM (
                        SELECT question_id
                        FROM answers
                        WHERE release_id = ?
                        GROUP BY question_id
                        HAVING COUNT(*) != 4
                    )
                    """.trimIndent(),
                    releaseArgument,
                ),
            )
            assertEquals(
                0L,
                db.count(
                    """
                    SELECT COUNT(*)
                    FROM questions AS q
                    LEFT JOIN answers AS a
                        ON a.release_id = q.release_id
                        AND a.question_id = q.question_id
                        AND a.answer_id = q.correct_answer_id
                    WHERE q.release_id = ? AND a.answer_id IS NULL
                    """.trimIndent(),
                    releaseArgument,
                ),
            )
            assertEquals(
                0L,
                db.count(
                    """
                    SELECT COUNT(*) FROM questions
                    WHERE release_id = ? AND TRIM(german_question_text) = ''
                    """.trimIndent(),
                    releaseArgument,
                ),
            )
            assertEquals(
                0L,
                db.count(
                    """
                    SELECT COUNT(*) FROM answers
                    WHERE release_id = ? AND TRIM(german_text) = ''
                    """.trimIndent(),
                    releaseArgument,
                ),
            )
            assertEquals(
                43L,
                db.count(
                    """
                    SELECT COUNT(DISTINCT q.question_id)
                    FROM questions AS q
                    LEFT JOIN answers AS a
                        ON a.release_id = q.release_id
                        AND a.question_id = q.question_id
                    WHERE q.release_id = ?
                        AND (
                            q.question_image_source_path IS NOT NULL
                            OR a.image_source_path IS NOT NULL
                        )
                    """.trimIndent(),
                    releaseArgument,
                ),
            )

            val questionIds = db.stringList(
                """
                SELECT question_id FROM questions
                WHERE release_id = ?
                ORDER BY scope, state_code, official_number, question_id
                """.trimIndent(),
                releaseArgument,
            )
            val installedLanguages = db.stringList(
                """
                SELECT language_code FROM content_packages
                WHERE release_id = ?
                ORDER BY language_code
                """.trimIndent(),
                releaseArgument,
            )

            assertEquals(460, questionIds.size)
            assertTrue("The German package must be installed", "de" in installedLanguages)
            assertTrue("No language package is installed", installedLanguages.isNotEmpty())

            installedLanguages.forEach { languageCode ->
                val languageArguments = arrayOf(release.releaseId, languageCode)
                assertEquals(
                    "$languageCode must contain 460 question translations",
                    460L,
                    db.count(
                        """
                        SELECT COUNT(*) FROM study_content
                        WHERE release_id = ? AND language_code = ?
                        """.trimIndent(),
                        languageArguments,
                    ),
                )
                assertEquals(
                    "$languageCode must contain 1840 translated answers",
                    1_840L,
                    db.count(
                        """
                        SELECT COUNT(*) FROM study_answers
                        WHERE release_id = ? AND language_code = ?
                        """.trimIndent(),
                        languageArguments,
                    ),
                )
                assertEquals(
                    "$languageCode contains blank questions or explanations",
                    0L,
                    db.count(
                        """
                        SELECT COUNT(*) FROM study_content
                        WHERE release_id = ? AND language_code = ?
                            AND (TRIM(question_text) = '' OR TRIM(explanation) = '')
                        """.trimIndent(),
                        languageArguments,
                    ),
                )
                assertEquals(
                    "$languageCode contains a question without exactly four answers",
                    0L,
                    db.count(
                        """
                        SELECT COUNT(*) FROM (
                            SELECT question_id
                            FROM study_answers
                            WHERE release_id = ? AND language_code = ?
                            GROUP BY question_id
                            HAVING COUNT(*) != 4
                        )
                        """.trimIndent(),
                        languageArguments,
                    ),
                )
            }

            questionIds to installedLanguages
        }

        val languageToExercise = installedLanguages.firstOrNull { it != "de" } ?: "de"
        questionIds.forEach { questionId ->
            val question = requireNotNull(
                repository.getStudyQuestion(
                    releaseId = release.releaseId,
                    questionId = questionId,
                    languageCode = languageToExercise,
                ),
            ) { "$questionId cannot be loaded for $languageToExercise" }
            assertEquals("$questionId must have four answers", 4, question.answers.size)
            assertTrue(
                "$questionId does not contain its declared correct answer",
                question.answers.any { answer -> answer.id == question.correctAnswerId },
            )
            assertTrue("$questionId has a blank German question", question.germanQuestionText.isNotBlank())
            assertTrue("$questionId has a blank localized question", question.localizedQuestionText.isNotBlank())
            assertTrue("$questionId has a blank explanation", question.explanation.isNotBlank())
        }

        val hesseSession = StudySessionCoordinator(repository).start(
            languageCode = languageToExercise,
            stateCode = "he",
        )
        assertEquals(
            "A state study session must contain 300 general and 10 state questions",
            310,
            hesseSession.questionIds.size,
        )

        println(
            "Verified release ${release.releaseId}: 460 questions, 1840 answers, " +
                "43 image-dependent questions, ${installedLanguages.size} installed language " +
                "package(s), and a 310-question Hessen study session.",
        )
    }
}

private fun SQLiteDatabase.count(
    sql: String,
    selectionArguments: Array<String>,
): Long = rawQuery(sql, selectionArguments).use { cursor ->
    check(cursor.moveToFirst()) { "Count query returned no row" }
    cursor.getLong(0)
}

private fun SQLiteDatabase.stringList(
    sql: String,
    selectionArguments: Array<String>,
): List<String> = rawQuery(sql, selectionArguments).use { cursor ->
    buildList {
        while (cursor.moveToNext()) {
            add(cursor.getString(0))
        }
    }
}

private fun SQLiteDatabase.stateQuestionCounts(releaseId: String): Map<String, Int> =
    rawQuery(
        """
        SELECT state_code, COUNT(*)
        FROM questions
        WHERE release_id = ? AND scope = 'STATE'
        GROUP BY state_code
        ORDER BY state_code
        """.trimIndent(),
        arrayOf(releaseId),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(cursor.getString(0), cursor.getInt(1))
            }
        }
    }

private fun expectedStateQuestionCounts(): Map<String, Int> =
    listOf(
        "BB", "BE", "BW", "BY", "HB", "HE", "HH", "MV",
        "NI", "NW", "RP", "SH", "SL", "SN", "ST", "TH",
    ).associateWith { 10 }
