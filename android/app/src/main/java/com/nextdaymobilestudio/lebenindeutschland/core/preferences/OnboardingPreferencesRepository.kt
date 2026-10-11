package com.nextdaymobilestudio.lebenindeutschland.core.preferences

import kotlinx.coroutines.flow.Flow

interface OnboardingPreferencesRepository {
    val preferences: Flow<OnboardingPreferences>

    suspend fun setLanguageCode(languageCode: String)

    suspend fun setStateCode(stateCode: String)
}
