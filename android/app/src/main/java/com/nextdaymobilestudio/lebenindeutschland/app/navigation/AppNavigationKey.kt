package com.nextdaymobilestudio.lebenindeutschland.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavigationKey : NavKey

@Serializable
data object LanguageSelectionKey : AppNavigationKey

@Serializable
data object StateSelectionKey : AppNavigationKey
