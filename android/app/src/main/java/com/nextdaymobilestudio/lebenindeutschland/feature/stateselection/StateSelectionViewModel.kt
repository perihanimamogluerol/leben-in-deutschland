package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection

import androidx.lifecycle.ViewModel
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StateSelectionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StateSelectionUiState())
    val uiState: StateFlow<StateSelectionUiState> = _uiState.asStateFlow()

    fun onStateSelected(state: GermanState) {
        _uiState.update { currentState ->
            currentState.copy(selectedState = state)
        }
    }
}
