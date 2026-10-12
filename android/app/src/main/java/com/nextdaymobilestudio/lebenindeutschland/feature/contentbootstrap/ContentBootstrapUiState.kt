package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap

sealed interface ContentBootstrapUiState {
    data object Loading : ContentBootstrapUiState

    data class Ready(
        val releaseId: String,
        val questionCount: Int,
        val isRefreshing: Boolean,
        val refreshFailed: Boolean = false,
    ) : ContentBootstrapUiState

    data object Error : ContentBootstrapUiState
}
