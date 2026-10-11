package com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui

object LanguageSelectionTestTags {
    const val SCREEN = "language_selection_screen"
    const val SEARCH = "language_selection_search"
    const val EMPTY_STATE = "language_selection_empty_state"
    const val CONTINUE = "language_selection_continue"

    fun option(languageCode: String): String = "language_selection_option_$languageCode"
}
