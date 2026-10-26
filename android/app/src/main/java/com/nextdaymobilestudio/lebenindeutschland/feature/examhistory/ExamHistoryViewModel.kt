package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextdaymobilestudio.lebenindeutschland.core.database.repository.LocalContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExamHistoryViewModel(
    localContentRepository: LocalContentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ExamHistoryUiState>(ExamHistoryUiState.Loading)
    val uiState: StateFlow<ExamHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localContentRepository.observeMockExamHistory()
                .catch { _uiState.value = ExamHistoryUiState.Error }
                .collectLatest { attempts ->
                    _uiState.value = if (attempts.isEmpty()) {
                        ExamHistoryUiState.Empty
                    } else {
                        ExamHistoryUiState.Content(attempts)
                    }
                }
        }
    }

    companion object {
        fun factory(
            localContentRepository: LocalContentRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExamHistoryViewModel(localContentRepository) as T
        }
    }
}
