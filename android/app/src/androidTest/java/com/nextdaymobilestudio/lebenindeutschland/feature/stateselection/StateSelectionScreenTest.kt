package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui.StateSelectionRoute
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui.StateSelectionScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui.StateSelectionTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialStateHasNoSelectionAndContinueIsDisabled() {
        setRoute()

        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("bw"))
            .assertIsNotSelected()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton,
                ),
            )
        composeRule
            .onNodeWithTag(StateSelectionTestTags.CONTINUE)
            .assertIsNotEnabled()
    }

    @Test
    fun clickingStateUpdatesSelectionAndEnablesContinue() {
        val state = mutableStateOf(StateSelectionUiState())

        composeRule.setContent {
            LebenInDeutschlandTheme {
                StateSelectionScreen(
                    uiState = state.value,
                    onBack = {},
                    onStateSelected = { selectedState ->
                        state.value = state.value.copy(selectedState = selectedState)
                    },
                    onContinue = {},
                )
            }
        }

        scrollToState("he")
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("he"))
            .performClick()

        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("he"))
            .assertIsSelected()
        composeRule
            .onNodeWithTag(StateSelectionTestTags.CONTINUE)
            .assertIsEnabled()
    }

    @Test
    fun selectingAnotherStateReplacesThePreviousSelection() {
        val state = mutableStateOf(StateSelectionUiState())

        composeRule.setContent {
            LebenInDeutschlandTheme {
                StateSelectionScreen(
                    uiState = state.value,
                    onBack = {},
                    onStateSelected = { selectedState ->
                        state.value = state.value.copy(selectedState = selectedState)
                    },
                    onContinue = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("be"))
            .performClick()
        scrollToState("hh")
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("hh"))
            .performClick()

        scrollToState("be")
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("be"))
            .assertIsNotSelected()
        scrollToState("hh")
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("hh"))
            .assertIsSelected()
    }

    @Test
    fun continueReturnsSelectedState() {
        var selectedState: GermanState? = null
        setRoute(onContinue = { state -> selectedState = state })

        scrollToState("he")
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("he"))
            .performClick()
        composeRule
            .onNodeWithTag(StateSelectionTestTags.CONTINUE)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(GermanState.HESSE, selectedState)
        }
    }

    @Test
    fun backButtonInvokesCallback() {
        var backInvoked = false
        setRoute(onBack = { backInvoked = true })

        composeRule
            .onNodeWithTag(StateSelectionTestTags.BACK)
            .performClick()

        composeRule.runOnIdle {
            assertTrue(backInvoked)
        }
    }

    @Test
    fun existingSelectionIsFullyVisibleWhenScreenOpens() {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                StateSelectionScreen(
                    uiState = StateSelectionUiState(
                        selectedState = GermanState.HESSE,
                    ),
                    onBack = {},
                    onStateSelected = {},
                    onContinue = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("he"))
            .assertIsDisplayed()
            .assertIsSelected()
    }

    @Test
    fun allSixteenStatesAreReachableByScrolling() {
        setRoute()

        scrollToState("th")
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("th"))
            .assertIsDisplayed()
    }

    @Test
    fun largeFontScaleKeepsCriticalControlsVisible() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 1.3f,
                ),
            ) {
                LebenInDeutschlandTheme {
                    StateSelectionScreen(
                        uiState = StateSelectionUiState(),
                        onBack = {},
                        onStateSelected = {},
                        onContinue = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(StateSelectionTestTags.BACK)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(StateSelectionTestTags.option("bw"))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(StateSelectionTestTags.CONTINUE)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    private fun setRoute(
        onBack: () -> Unit = {},
        onContinue: (GermanState) -> Unit = {},
    ) {
        val viewModel = StateSelectionViewModel()
        composeRule.setContent {
            LebenInDeutschlandTheme {
                StateSelectionRoute(
                    onBack = onBack,
                    onContinue = onContinue,
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun scrollToState(stateCode: String) {
        composeRule
            .onNodeWithTag(StateSelectionTestTags.LIST)
            .performScrollToNode(
                hasTestTag(StateSelectionTestTags.option(stateCode)),
            )
    }
}
