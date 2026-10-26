package com.nextdaymobilestudio.lebenindeutschland.feature.settings.ui

import androidx.compose.runtime.Composable

@Composable
fun SettingsRoute(
    languageName: String,
    stateName: String,
    onBack: () -> Unit,
    onChangeLanguage: () -> Unit,
    onChangeState: () -> Unit,
) {
    SettingsScreen(
        languageName = languageName,
        stateName = stateName,
        onBack = onBack,
        onChangeLanguage = onChangeLanguage,
        onChangeState = onChangeState,
    )
}
