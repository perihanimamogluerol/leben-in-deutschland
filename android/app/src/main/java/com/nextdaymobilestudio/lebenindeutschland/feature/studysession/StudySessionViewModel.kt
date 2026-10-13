package com.nextdaymobilestudio.lebenindeutschland.feature.studysession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudySessionViewModel(
    private val languageCode: String,
    private val stateCode: String?,
    private val coordinator: StudySessionCoordinator,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StudySessionUiState>(
        StudySessionUiState.Loading,
    )
    val uiState: StateFlow<StudySessionUiState> = _uiState.asStateFlow()

    private var session: StudySession? = null
    private var loadingJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.value = StudySessionUiState.Loading
            try {
                val loadedSession = coordinator.start(languageCode, stateCode)
                session = loadedSession
                showQuestion(loadedSession, index = 0)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.value = StudySessionUiState.Error
            }
        }
    }

    fun selectAnswer(answerId: String) {
        val current = _uiState.value as? StudySessionUiState.Ready ?: return
        if (!current.isSubmitted) {
            _uiState.value = current.copy(selectedAnswerId = answerId)
        }
    }

    fun submitAnswer() {
        val current = _uiState.value as? StudySessionUiState.Ready ?: return
        if (current.selectedAnswerId != null) {
            _uiState.value = current.copy(isSubmitted = true)
        }
    }

    fun nextQuestion() {
        val current = _uiState.value as? StudySessionUiState.Ready ?: return
        if (!current.isSubmitted) return
        if (current.isLastQuestion) {
            _uiState.value = StudySessionUiState.Complete(current.totalCount)
            return
        }
        val loadedSession = session ?: return
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.value = StudySessionUiState.Loading
            try {
                showQuestion(loadedSession, current.currentIndex + 1)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.value = StudySessionUiState.Error
            }
        }
    }

    private suspend fun showQuestion(
        loadedSession: StudySession,
        index: Int,
    ) {
        _uiState.value = StudySessionUiState.Ready(
            question = coordinator.getQuestion(loadedSession, index),
            currentIndex = index,
            totalCount = loadedSession.questionIds.size,
        )
    }

    companion object {
        fun factory(
            languageCode: String,
            stateCode: String?,
            localContentRepository: LocalContentRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StudySessionViewModel(
                    languageCode = languageCode,
                    stateCode = stateCode,
                    coordinator = StudySessionCoordinator(localContentRepository),
                ) as T
        }
    }
}
