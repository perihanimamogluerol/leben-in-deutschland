package com.nextdaymobilestudio.lebenindeutschland.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nextdaymobilestudio.lebenindeutschland.core.database.dao.ContentDao
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.AnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentPackageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.ContentReleaseLanguageEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.QuestionEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyAnswerEntity
import com.nextdaymobilestudio.lebenindeutschland.core.database.entity.StudyContentEntity
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
    ],
    version = 1,
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
            ).build()
    }
}
