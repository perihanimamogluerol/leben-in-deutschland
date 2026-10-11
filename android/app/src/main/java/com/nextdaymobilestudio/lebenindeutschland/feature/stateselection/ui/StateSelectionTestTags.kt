package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.ui

object StateSelectionTestTags {
    const val SCREEN = "state_selection_screen"
    const val LIST = "state_selection_list"
    const val BACK = "state_selection_back"
    const val CONTINUE = "state_selection_continue"

    fun option(stateCode: String): String = "state_selection_option_$stateCode"
}
