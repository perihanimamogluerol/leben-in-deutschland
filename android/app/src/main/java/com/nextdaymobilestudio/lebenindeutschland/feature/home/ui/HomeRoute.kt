package com.nextdaymobilestudio.lebenindeutschland.feature.home.ui

import androidx.compose.runtime.Composable

@Composable
fun HomeRoute(
    questionCount: Int,
    languageName: String,
    stateName: String,
    onStartStudy: () -> Unit,
) {
    HomeScreen(
        questionCount = questionCount,
        languageName = languageName,
        stateName = stateName,
        onStartStudy = onStartStudy,
    )
}
