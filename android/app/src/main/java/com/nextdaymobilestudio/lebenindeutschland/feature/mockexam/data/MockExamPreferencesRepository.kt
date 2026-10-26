package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.data

import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.MockExamSnapshot
import kotlinx.coroutines.flow.Flow

interface MockExamPreferencesRepository {
    val exam: Flow<MockExamSnapshot?>

    suspend fun getExam(): MockExamSnapshot?

    suspend fun saveExam(exam: MockExamSnapshot)

    suspend fun clearExam()
}
