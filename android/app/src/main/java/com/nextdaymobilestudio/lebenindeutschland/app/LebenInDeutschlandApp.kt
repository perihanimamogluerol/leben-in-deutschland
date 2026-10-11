package com.nextdaymobilestudio.lebenindeutschland.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.AppNavigationKey
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.LanguageSelectionKey
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.StateSelectionKey
import com.nextdaymobilestudio.lebenindeutschland.core.preferences.OnboardingPreferences
import com.nextdaymobilestudio.lebenindeutschland.core.preferences.OnboardingPreferencesRepository
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionRoute
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model.GermanState
import com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui.StateSelectionRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun LebenInDeutschlandApp(
    onboardingPreferencesRepository: OnboardingPreferencesRepository,
    onApplicationLanguageSelected: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nullablePreferences = remember(onboardingPreferencesRepository) {
        onboardingPreferencesRepository.preferences.asNullable()
    }
    val preferences by nullablePreferences.collectAsStateWithLifecycle(initialValue = null)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val loadedPreferences = preferences
        if (loadedPreferences == null) {
            CircularProgressIndicator()
        } else {
            OnboardingNavigation(
                preferences = loadedPreferences,
                repository = onboardingPreferencesRepository,
                onApplicationLanguageSelected = onApplicationLanguageSelected,
            )
        }
    }
}

@Composable
private fun OnboardingNavigation(
    preferences: OnboardingPreferences,
    repository: OnboardingPreferencesRepository,
    onApplicationLanguageSelected: (SupportedLanguage) -> Unit,
) {
    val persistedLanguage = SupportedLanguage.fromCode(preferences.languageCode)
    val persistedState = GermanState.fromCode(preferences.stateCode)
    val initialDestination: AppNavigationKey = remember {
        if (persistedLanguage == null) {
            LanguageSelectionKey
        } else {
            StateSelectionKey
        }
    }
    val backStack = rememberNavBackStack(initialDestination)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val stateConfirmationTemplate = stringResource(
        R.string.state_selection_temporary_confirmation,
    )
    val navigateBack: () -> Unit = {
        when {
            backStack.size > 1 -> backStack.removeLastOrNull()
            backStack.lastOrNull() == StateSelectionKey -> {
                backStack.removeLastOrNull()
                backStack.add(LanguageSelectionKey)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = navigateBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<LanguageSelectionKey> {
                    LanguageSelectionRoute(
                        persistedLanguage = persistedLanguage,
                        onContinue = { language ->
                            coroutineScope.launch {
                                repository.setLanguageCode(language.code)
                                if (backStack.lastOrNull() != StateSelectionKey) {
                                    backStack.add(StateSelectionKey)
                                }
                                onApplicationLanguageSelected(language)
                            }
                        },
                    )
                }
                entry<StateSelectionKey> {
                    StateSelectionRoute(
                        initialState = persistedState,
                        onBack = navigateBack,
                        onContinue = { state ->
                            coroutineScope.launch {
                                repository.setStateCode(state.code)
                                snackbarHostState.showSnackbar(
                                    message = stateConfirmationTemplate.format(
                                        state.germanName,
                                    ),
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

private fun Flow<OnboardingPreferences>.asNullable(): Flow<OnboardingPreferences?> =
    map { preferences -> preferences }
