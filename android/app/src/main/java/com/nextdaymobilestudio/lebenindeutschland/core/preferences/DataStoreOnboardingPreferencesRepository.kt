package com.nextdaymobilestudio.lebenindeutschland.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val ONBOARDING_PREFERENCES_NAME = "onboarding_preferences"

internal val Context.onboardingPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ONBOARDING_PREFERENCES_NAME,
)

class DataStoreOnboardingPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : OnboardingPreferencesRepository {
    override val preferences: Flow<OnboardingPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { values ->
            OnboardingPreferences(
                languageCode = values[Keys.SELECTED_LANGUAGE_CODE],
                stateCode = values[Keys.SELECTED_STATE_CODE],
            )
        }

    override suspend fun setLanguageCode(languageCode: String) {
        dataStore.edit { values ->
            values[Keys.SELECTED_LANGUAGE_CODE] = languageCode
        }
    }

    override suspend fun setStateCode(stateCode: String) {
        dataStore.edit { values ->
            values[Keys.SELECTED_STATE_CODE] = stateCode
        }
    }

    internal suspend fun clear() {
        dataStore.edit { values -> values.clear() }
    }

    private object Keys {
        val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
        val SELECTED_STATE_CODE = stringPreferencesKey("selected_state_code")
    }
}

fun Context.createOnboardingPreferencesRepository(): OnboardingPreferencesRepository =
    DataStoreOnboardingPreferencesRepository(
        dataStore = applicationContext.onboardingPreferencesDataStore,
    )
