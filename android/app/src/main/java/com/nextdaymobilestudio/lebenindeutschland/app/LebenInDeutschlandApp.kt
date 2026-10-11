package com.nextdaymobilestudio.lebenindeutschland.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.LanguageSelectionKey
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.StateSelectionKey
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionRoute
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui.StateSelectionRoute
import kotlinx.coroutines.launch

@Composable
fun LebenInDeutschlandApp(
    onApplicationLanguageSelected: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(LanguageSelectionKey)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val stateConfirmationTemplate = stringResource(
        R.string.state_selection_temporary_confirmation,
    )

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<LanguageSelectionKey> {
                    LanguageSelectionRoute(
                        onContinue = { language ->
                            if (backStack.lastOrNull() != StateSelectionKey) {
                                backStack.add(StateSelectionKey)
                            }
                            onApplicationLanguageSelected(language)
                        },
                    )
                }
                entry<StateSelectionKey> {
                    StateSelectionRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onContinue = { state ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = stateConfirmationTemplate.format(state.germanName),
                                )
                            }
                        },
                    )
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 112.dp),
        )
    }
}
