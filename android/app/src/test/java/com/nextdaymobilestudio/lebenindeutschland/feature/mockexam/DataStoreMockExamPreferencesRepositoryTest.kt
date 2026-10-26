package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.data.DataStoreMockExamPreferencesRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreMockExamPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: DataStoreMockExamPreferencesRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repository = DataStoreMockExamPreferencesRepository(
            dataStore = PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = {
                    File(temporaryFolder.root, "mock-exam.preferences_pb")
                },
            ),
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `exam survives serialization with answers and deadline`() = runBlocking {
        val expected = MockExamSnapshot(
            releaseId = "release-3",
            languageCode = "tr",
            stateCode = "HE",
            questionIds = (1..33).map { "question_$it" },
            selectedAnswers = mapOf("question_1" to "A", "question_2" to "C"),
            currentIndex = 6,
            startedAtEpochMillis = 1_000L,
            deadlineEpochMillis = 3_601_000L,
        )

        repository.saveExam(expected)

        assertEquals(expected, repository.getExam())
        assertEquals(expected, repository.exam.first())
    }

    @Test
    fun `clear removes persisted exam`() = runBlocking {
        repository.saveExam(
            MockExamSnapshot(
                releaseId = "release-3",
                languageCode = "de",
                stateCode = "BE",
                questionIds = (1..33).map { "question_$it" },
                startedAtEpochMillis = 1_000L,
                deadlineEpochMillis = 3_601_000L,
            ),
        )

        repository.clearExam()

        assertNull(repository.getExam())
    }
}
