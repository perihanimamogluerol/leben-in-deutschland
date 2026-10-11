package com.nextdaymobilestudio.lebenindeutschland.core.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreOnboardingPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: DataStoreOnboardingPreferencesRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = {
                File(temporaryFolder.root, "onboarding.preferences_pb")
            },
        )
        repository = DataStoreOnboardingPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `new repository has no saved selections`() = runBlocking {
        assertEquals(
            OnboardingPreferences(),
            repository.preferences.first(),
        )
    }

    @Test
    fun `language and state selections are persisted`() = runBlocking {
        repository.setLanguageCode("tr")
        repository.setStateCode("he")

        assertEquals(
            OnboardingPreferences(
                languageCode = "tr",
                stateCode = "he",
            ),
            repository.preferences.first(),
        )
    }

    @Test
    fun `changing language preserves the selected state`() = runBlocking {
        repository.setLanguageCode("de")
        repository.setStateCode("be")

        repository.setLanguageCode("en")

        assertEquals(
            OnboardingPreferences(
                languageCode = "en",
                stateCode = "be",
            ),
            repository.preferences.first(),
        )
    }
}
