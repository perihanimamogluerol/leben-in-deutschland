package com.nextdaymobilestudio.lebenindeutschland.app

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.AppNavigationKey
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.ContentBootstrapKey
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.LanguageSelectionKey
import com.nextdaymobilestudio.lebenindeutschland.app.navigation.StateSelectionKey
import com.nextdaymobilestudio.lebenindeutschland.core.content.sync.ContentSynchronizer
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import com.nextdaymobilestudio.lebenindeutschland.core.preferences.OnboardingPreferences
import com.nextdaymobilestudio.lebenindeutschland.core.preferences.OnboardingPreferencesRepository
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ContentBootstrapCoordinator
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ui.ContentBootstrapRoute
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
    localContentRepository: LocalContentRepository,
    contentSynchronizerProvider: () -> ContentSynchronizer,
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
                localContentRepository = localContentRepository,
                contentSynchronizerProvider = contentSynchronizerProvider,
                onApplicationLanguageSelected = onApplicationLanguageSelected,
            )
        }
    }
}

@Composable
private fun OnboardingNavigation(
    preferences: OnboardingPreferences,
    repository: OnboardingPreferencesRepository,
    localContentRepository: LocalContentRepository,
    contentSynchronizerProvider: () -> ContentSynchronizer,
    onApplicationLanguageSelected: (SupportedLanguage) -> Unit,
) {
    val persistedLanguage = SupportedLanguage.fromCode(preferences.languageCode)
    val persistedState = GermanState.fromCode(preferences.stateCode)
    val initialDestination: AppNavigationKey = remember {
        when {
            persistedLanguage == null -> LanguageSelectionKey
            persistedState == null -> StateSelectionKey
            else -> ContentBootstrapKey
        }
    }
    val backStack = rememberNavBackStack(initialDestination)
    val coroutineScope = rememberCoroutineScope()
    val contentBootstrapCoordinator = remember(
        localContentRepository,
        contentSynchronizerProvider,
    ) {
        ContentBootstrapCoordinator(
            localContentRepository = localContentRepository,
            contentSynchronizerProvider = contentSynchronizerProvider,
            reportError = { message, throwable ->
                Log.e(CONTENT_BOOTSTRAP_LOG_TAG, message, throwable)
            },
        )
    }
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
                                if (backStack.lastOrNull() != ContentBootstrapKey) {
                                    backStack.add(ContentBootstrapKey)
                                }
                            }
                        },
                    )
                }
                entry<ContentBootstrapKey> {
                    ContentBootstrapRoute(
                        selectedLanguageCode = persistedLanguage?.code
                            ?: SupportedLanguage.GERMAN.code,
                        coordinator = contentBootstrapCoordinator,
                    )
                }
            },
        )
    }
}

private fun Flow<OnboardingPreferences>.asNullable(): Flow<OnboardingPreferences?> =
    map { preferences -> preferences }

private const val CONTENT_BOOTSTRAP_LOG_TAG = "ContentBootstrap"
