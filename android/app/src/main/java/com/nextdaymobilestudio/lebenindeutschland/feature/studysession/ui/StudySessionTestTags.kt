package com.nextdaymobilestudio.lebenindeutschland.feature.studysession.ui

object StudySessionTestTags {
    const val SCREEN = "study-session-screen"
    const val LOADING = "study-session-loading"
    const val ERROR = "study-session-error"
    const val QUESTION = "study-session-question"
    const val EXPLANATION = "study-session-explanation"
    const val ACTION = "study-session-action"
    const val COMPLETE = "study-session-complete"
    const val RETRY = "study-session-retry"
    const val BACK_HOME = "study-session-back-home"

    fun answer(answerId: String): String = "study-session-answer-$answerId"
}
