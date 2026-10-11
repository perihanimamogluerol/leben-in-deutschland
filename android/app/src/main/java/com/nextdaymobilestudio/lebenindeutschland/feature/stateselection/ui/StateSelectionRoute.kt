package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.StateSelectionViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState

@Composable
fun StateSelectionRoute(
    onBack: () -> Unit,
    onContinue: (GermanState) -> Unit,
    viewModel: StateSelectionViewModel? = null,
) {
    val actualViewModel = viewModel ?: composeViewModel<StateSelectionViewModel>()
    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    StateSelectionScreen(
        uiState = uiState,
        onBack = onBack,
        onStateSelected = actualViewModel::onStateSelected,
        onContinue = {
            uiState.selectedState?.let(onContinue)
        },
    )
}
