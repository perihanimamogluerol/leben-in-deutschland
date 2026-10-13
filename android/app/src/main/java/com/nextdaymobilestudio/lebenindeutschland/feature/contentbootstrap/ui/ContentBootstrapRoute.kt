package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapCoordinator
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapUiState
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapViewModel

@Composable
fun ContentBootstrapRoute(
    selectedLanguageCode: String,
    coordinator: ContentBootstrapCoordinator,
    onContentReady: (questionCount: Int) -> Unit,
    viewModel: ContentBootstrapViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<ContentBootstrapViewModel>(
        factory = ContentBootstrapViewModel.factory(
            selectedLanguageCode = selectedLanguageCode,
            coordinator = coordinator,
        ),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val readyState = uiState as? ContentBootstrapUiState.Ready
        if (readyState != null && !readyState.isRefreshing) {
            onContentReady(readyState.questionCount)
        }
    }

    ContentBootstrapScreen(
        uiState = uiState,
        onRetry = actualViewModel::retry,
    )
}
