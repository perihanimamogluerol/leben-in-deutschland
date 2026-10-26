package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.MockExamSnapshot
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MOCK_EXAM_PREFERENCES_NAME = "mock_exam_preferences"

internal val Context.mockExamPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = MOCK_EXAM_PREFERENCES_NAME,
)

class DataStoreMockExamPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) : MockExamPreferencesRepository {
    override val exam: Flow<MockExamSnapshot?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { values ->
            values[Keys.ACTIVE_EXAM]?.let { encoded ->
                runCatching {
                    json.decodeFromString<MockExamSnapshot>(encoded)
                }.getOrNull()
            }
        }

    override suspend fun getExam(): MockExamSnapshot? = exam.first()

    override suspend fun saveExam(exam: MockExamSnapshot) {
        dataStore.edit { values ->
            values[Keys.ACTIVE_EXAM] = json.encodeToString(exam)
        }
    }

    override suspend fun clearExam() {
        dataStore.edit { values -> values.remove(Keys.ACTIVE_EXAM) }
    }

    private object Keys {
        val ACTIVE_EXAM = stringPreferencesKey("active_mock_exam")
    }
}

fun Context.createMockExamPreferencesRepository(): MockExamPreferencesRepository =
    DataStoreMockExamPreferencesRepository(
        dataStore = applicationContext.mockExamPreferencesDataStore,
    )
