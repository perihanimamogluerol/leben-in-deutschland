package com.nextdaymobilestudio.lebenindeutschland.feature.studysession.ui

object StudySessionTestTags {
    const val SCREEN = "study-session-screen"
    const val LOADING = "study-session-loading"
    const val ERROR = "study-session-error"
    const val QUESTION = "study-session-question"
    const val EXPLANATION = "study-session-explanation"
    const val MEMORY_KEY_TOGGLE = "study-session-memory-key-toggle"
    const val MEMORY_KEY = "study-session-memory-key"
    const val ACTION = "study-session-action"
    const val COMPLETE = "study-session-complete"
    const val EMPTY_WRONG_ANSWERS = "study-session-empty-wrong-answers"
    const val RETRY = "study-session-retry"
    const val BACK_HOME = "study-session-back-home"

    fun answer(answerId: String): String = "study-session-answer-$answerId"
}
