package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapCoordinator
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapViewModel

@Composable
fun ContentBootstrapRoute(
    selectedLanguageCode: String,
    coordinator: ContentBootstrapCoordinator,
    viewModel: ContentBootstrapViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<ContentBootstrapViewModel>(
        factory = ContentBootstrapViewModel.factory(
            selectedLanguageCode = selectedLanguageCode,
            coordinator = coordinator,
        ),
    )
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    ContentBootstrapScreen(
        uiState = uiState,
        onRetry = actualViewModel::retry,
    )
}
