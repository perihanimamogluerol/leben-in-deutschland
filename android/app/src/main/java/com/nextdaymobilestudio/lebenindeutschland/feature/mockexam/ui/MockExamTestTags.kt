package com.nextdaymobilestudio.lebenindeutschland.feature.mockexam.ui

object MockExamTestTags {
    const val SCREEN = "mock-exam-screen"
    const val SETUP = "mock-exam-setup"
    const val START = "mock-exam-start"
    const val TIMER = "mock-exam-timer"
    const val QUESTION = "mock-exam-question"
    const val PREVIOUS = "mock-exam-previous"
    const val NEXT = "mock-exam-next"
    const val FINISH = "mock-exam-finish"
    const val CONFIRM_DIALOG = "mock-exam-confirm-dialog"
    const val RESULT = "mock-exam-result"

    fun answer(answerId: String): String = "mock-exam-answer-$answerId"
}
