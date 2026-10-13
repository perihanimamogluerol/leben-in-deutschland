package com.nextdaymobilestudio.lebenindeutschland.feature.studysession.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.feature.studysession.StudySessionViewModel

@Composable
fun StudySessionRoute(
    selectedLanguageCode: String,
    selectedStateCode: String?,
    localContentRepository: LocalContentRepository,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: StudySessionViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<StudySessionViewModel>(
        factory = StudySessionViewModel.factory(
            languageCode = selectedLanguageCode,
            stateCode = selectedStateCode,
            localContentRepository = localContentRepository,
        ),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    StudySessionScreen(
        uiState = uiState,
        onBack = onBack,
        onAnswerSelected = actualViewModel::selectAnswer,
        onSubmitAnswer = actualViewModel::submitAnswer,
        onNextQuestion = actualViewModel::nextQuestion,
        onRetry = actualViewModel::retry,
        onFinished = onFinished,
    )
}
