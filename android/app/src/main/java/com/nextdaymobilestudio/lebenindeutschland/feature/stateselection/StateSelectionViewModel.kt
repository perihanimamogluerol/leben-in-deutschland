package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StateSelectionViewModel(
    initialState: GermanState? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        StateSelectionUiState(selectedState = initialState),
    )
    val uiState: StateFlow<StateSelectionUiState> = _uiState.asStateFlow()

    fun onStateSelected(state: GermanState) {
        _uiState.update { currentState ->
            currentState.copy(selectedState = state)
        }
    }

    companion object {
        fun factory(initialState: GermanState?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    StateSelectionViewModel(initialState = initialState)
                }
            }
    }
}
