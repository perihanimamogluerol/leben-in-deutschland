package com.nextdaymobilestudio.lebenindeutschland.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextdaymobilestudio.lebenindeutschland.core.database.dao.ContentDao
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.MockExamAttemptEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.MockExamQuestionResultEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyQuestionProgressEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAnswerAuditEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.TranslationAuditEntity

@Database(
    entities = [
        ContentReleaseEntity::class,
        ContentReleaseLanguageEntity::class,
        ContentPackageEntity::class,
        QuestionEntity::class,
        AnswerEntity::class,
        StudyContentEntity::class,
        StudyAnswerEntity::class,
        TranslationAuditEntity::class,
        TranslationAnswerAuditEntity::class,
        StudyQuestionProgressEntity::class,
        MockExamAttemptEntity::class,
        MockExamQuestionResultEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LocalContentDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    companion object {
        const val DATABASE_NAME = "leben_in_deutschland_content.db"

        fun create(context: Context): LocalContentDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LocalContentDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_question_progress` (
                        `release_id` TEXT NOT NULL,
                        `language_code` TEXT NOT NULL,
                        `state_code` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `selected_answer_id` TEXT NOT NULL,
                        `is_correct` INTEGER NOT NULL,
                        `answered_at_epoch_millis` INTEGER NOT NULL,
                        `attempt_count` INTEGER NOT NULL,
                        PRIMARY KEY(`release_id`, `language_code`, `state_code`, `question_id`),
                        FOREIGN KEY(`release_id`, `question_id`)
                            REFERENCES `questions`(`release_id`, `question_id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                        `index_study_question_progress_release_id_question_id`
                    ON `study_question_progress` (`release_id`, `question_id`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                        `index_study_question_progress_release_id_language_code_state_code`
                    ON `study_question_progress` (`release_id`, `language_code`, `state_code`)
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mock_exam_attempts` (
                        `attempt_id` TEXT NOT NULL,
                        `release_id` TEXT NOT NULL,
                        `language_code` TEXT NOT NULL,
                        `state_code` TEXT NOT NULL,
                        `started_at_epoch_millis` INTEGER NOT NULL,
                        `submitted_at_epoch_millis` INTEGER NOT NULL,
                        `deadline_epoch_millis` INTEGER NOT NULL,
                        `correct_count` INTEGER NOT NULL,
                        `wrong_count` INTEGER NOT NULL,
                        `unanswered_count` INTEGER NOT NULL,
                        `total_count` INTEGER NOT NULL,
                        `submitted_by_timeout` INTEGER NOT NULL,
                        PRIMARY KEY(`attempt_id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                        `index_mock_exam_attempts_submitted_at_epoch_millis`
                    ON `mock_exam_attempts` (`submitted_at_epoch_millis`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                        `index_mock_exam_attempts_language_code_state_code_submitted_at_epoch_millis`
                    ON `mock_exam_attempts`
                        (`language_code`, `state_code`, `submitted_at_epoch_millis`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mock_exam_question_results` (
                        `attempt_id` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `selected_answer_id` TEXT,
                        `correct_answer_id` TEXT NOT NULL,
                        PRIMARY KEY(`attempt_id`, `question_id`),
                        FOREIGN KEY(`attempt_id`)
                            REFERENCES `mock_exam_attempts`(`attempt_id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                        `index_mock_exam_question_results_attempt_id`
                    ON `mock_exam_question_results` (`attempt_id`)
                    """.trimIndent(),
                )
            }
        }
    }
}
