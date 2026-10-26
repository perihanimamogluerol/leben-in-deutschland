package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.MockExamViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.data.MockExamPreferencesRepository

@Composable
fun MockExamRoute(
    selectedLanguageCode: String,
    selectedStateCode: String?,
    localContentRepository: LocalContentRepository,
    mockExamPreferencesRepository: MockExamPreferencesRepository,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: MockExamViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<MockExamViewModel>(
        factory = MockExamViewModel.factory(
            languageCode = selectedLanguageCode,
            stateCode = selectedStateCode,
            localContentRepository = localContentRepository,
            mockExamPreferencesRepository = mockExamPreferencesRepository,
        ),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    MockExamScreen(
        uiState = uiState,
        onBack = onBack,
        onStartExam = actualViewModel::startExam,
        onAnswerSelected = actualViewModel::selectAnswer,
        onPrevious = actualViewModel::previousQuestion,
        onNext = actualViewModel::nextQuestion,
        onRequestSubmit = actualViewModel::requestSubmit,
        onCancelSubmit = actualViewModel::cancelSubmit,
        onConfirmSubmit = actualViewModel::confirmSubmit,
        onRetry = actualViewModel::retry,
        onStartNewExam = actualViewModel::startNewExam,
        onFinished = onFinished,
    )
}
