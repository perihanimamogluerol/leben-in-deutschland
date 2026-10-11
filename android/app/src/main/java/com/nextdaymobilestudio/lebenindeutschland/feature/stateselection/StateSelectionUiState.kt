package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection

import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState

data class StateSelectionUiState(
    val selectedState: GermanState? = null,
    val states: List<GermanState> = GermanState.entries,
) {
    val canContinue: Boolean
        get() = selectedState != null
}
