package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection

import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateSelectionViewModelTest {
    @Test
    fun `initial state contains all states without a selection`() {
        val state = StateSelectionViewModel().uiState.value

        assertEquals(16, state.states.size)
        assertNull(state.selectedState)
        assertFalse(state.canContinue)
    }

    @Test
    fun `selecting a state enables continue`() {
        val viewModel = StateSelectionViewModel()

        viewModel.onStateSelected(GermanState.HESSE)

        assertEquals(GermanState.HESSE, viewModel.uiState.value.selectedState)
        assertTrue(viewModel.uiState.value.canContinue)
    }

    @Test
    fun `selecting another state replaces the previous selection`() {
        val viewModel = StateSelectionViewModel()

        viewModel.onStateSelected(GermanState.BERLIN)
        viewModel.onStateSelected(GermanState.HAMBURG)

        assertEquals(GermanState.HAMBURG, viewModel.uiState.value.selectedState)
    }
}
