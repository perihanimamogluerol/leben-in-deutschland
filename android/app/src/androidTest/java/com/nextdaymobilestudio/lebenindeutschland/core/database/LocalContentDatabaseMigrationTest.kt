package com.nextdaymobilestudio.lebenindeutschland.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalContentDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LocalContentDatabase::class.java,
    )

    @Test
    fun migration1To2CreatesStudyProgressWithoutDroppingContent() {
        helper.createDatabase(V1_TEST_DATABASE, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO content_releases (
                    release_id, schema_version, dataset_version, package_strategy,
                    status, created_at, source_catalog_sha256, catalog_sha256,
                    question_count, chunk_size, language_count, quality_issue_count,
                    installed_at_epoch_millis, is_active
                ) VALUES (
                    'release-3', 3, '2026-10', 'SELF_CONTAINED_LANGUAGE_PACK',
                    'READY', '2026-10-13T00:00:00Z', 'source', 'catalog',
                    460, 25, 21, 0, 1, 1
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            V1_TEST_DATABASE,
            2,
            true,
            LocalContentDatabase.MIGRATION_1_2,
        ).use { database ->
            database.query("SELECT COUNT(*) FROM content_releases").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            database.query(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'table' AND name = 'study_question_progress'
                """.trimIndent(),
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migration2To3CreatesMockExamHistoryWithoutDroppingContent() {
        helper.createDatabase(V2_TEST_DATABASE, 2).use { database ->
            database.execSQL(
                """
                INSERT INTO content_releases (
                    release_id, schema_version, dataset_version, package_strategy,
                    status, created_at, source_catalog_sha256, catalog_sha256,
                    question_count, chunk_size, language_count, quality_issue_count,
                    installed_at_epoch_millis, is_active
                ) VALUES (
                    'release-3', 3, '2026-10', 'SELF_CONTAINED_LANGUAGE_PACK',
                    'READY', '2026-10-13T00:00:00Z', 'source', 'catalog',
                    460, 25, 21, 0, 1, 1
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            V2_TEST_DATABASE,
            3,
            true,
            LocalContentDatabase.MIGRATION_2_3,
        ).use { database ->
            database.query("SELECT COUNT(*) FROM content_releases").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            listOf("mock_exam_attempts", "mock_exam_question_results").forEach { table ->
                database.query(
                    """
                    SELECT COUNT(*) FROM sqlite_master
                    WHERE type = 'table' AND name = '$table'
                    """.trimIndent(),
                ).use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(1, cursor.getInt(0))
                }
            }
        }
    }

    private companion object {
        const val V1_TEST_DATABASE = "study-progress-migration-test"
        const val V2_TEST_DATABASE = "mock-exam-history-migration-test"
    }
}
