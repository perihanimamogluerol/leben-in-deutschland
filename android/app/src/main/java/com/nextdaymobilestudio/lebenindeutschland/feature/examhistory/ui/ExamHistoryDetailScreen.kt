package com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.core.database.model.MockExamHistoryItem
import com.nextdaymobilestudio.lebenindeutschland.feature.examhistory.ExamHistoryDetailUiState

@Composable
fun ExamHistoryDetailScreen(
    uiState: ExamHistoryDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ExamHistoryTestTags.DETAIL_SCREEN),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExamHistoryTopBar(
                title = stringResource(R.string.exam_history_detail_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        when (uiState) {
            ExamHistoryDetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            ExamHistoryDetailUiState.NotFound,
            ExamHistoryDetailUiState.Error -> DetailUnavailable(innerPadding)
            is ExamHistoryDetailUiState.Content -> DetailContent(
                state = uiState,
                innerPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun DetailContent(
    state: ExamHistoryDetailUiState.Content,
    innerPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag(ExamHistoryTestTags.QUESTION_LIST),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ExamResultSummary(state.result.summary)
        }
        items(
            items = state.questions,
            key = { item -> item.result.questionId },
        ) { item ->
            ExamQuestionResultCard(
                item = item,
                totalCount = state.questions.size,
            )
        }
    }
}

@Composable
private fun ExamResultSummary(summary: MockExamHistoryItem) {
    val passed = summary.lifeInGermanyPassed
    val durationMinutes = (summary.durationSeconds / 60L).coerceAtLeast(1L)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ExamHistoryTestTags.DETAIL_SUMMARY),
        shape = RoundedCornerShape(22.dp),
        color = if (passed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (passed) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Warning
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(
                    if (passed) {
                        R.string.mock_exam_passed_title
                    } else {
                        R.string.mock_exam_failed_title
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.mock_exam_result_score,
                    summary.correctCount,
                    summary.totalCount,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.mock_exam_result_breakdown,
                    summary.correctCount,
                    summary.wrongCount,
                    summary.unansweredCount,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.exam_history_duration, durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
            )
            ResultThresholdRow(
                label = stringResource(R.string.mock_exam_lid_title),
                passed = summary.lifeInGermanyPassed,
            )
            ResultThresholdRow(
                label = stringResource(R.string.mock_exam_naturalization_title),
                passed = summary.naturalizationPassed,
            )
            if (summary.submittedByTimeout) {
                Text(
                    text = stringResource(R.string.mock_exam_time_up),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ResultThresholdRow(
    label: String,
    passed: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (passed) {
                        R.string.mock_exam_passed_title
                    } else {
                        R.string.mock_exam_failed_title
                    },
                ),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ExamQuestionResultCard(
    item: ExamHistoryDetailUiState.QuestionItem,
    totalCount: Int,
) {
    val result = item.result
    val question = item.question
    val statusResource = when {
        !result.isAnswered -> R.string.exam_history_unanswered
        result.isCorrect -> R.string.exam_history_correct
        else -> R.string.exam_history_wrong
    }
    val containerColor = when {
        !result.isAnswered -> MaterialTheme.colorScheme.surfaceVariant
        result.isCorrect -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val selectedAnswer = question?.answers
        ?.firstOrNull { answer -> answer.id == result.selectedAnswerId }
        ?.localizedText
    val correctAnswer = question?.answers
        ?.firstOrNull { answer -> answer.id == result.correctAnswerId }
        ?.localizedText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ExamHistoryTestTags.question(result.questionId)),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.mock_exam_progress,
                        result.position + 1,
                        totalCount,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(statusResource),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (question == null) {
                Text(
                    text = stringResource(R.string.exam_history_content_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = question.germanQuestionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (question.localizedQuestionText != question.germanQuestionText) {
                    Text(
                        text = question.localizedQuestionText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.exam_history_your_answer,
                        selectedAnswer ?: stringResource(R.string.exam_history_unanswered),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!result.isCorrect) {
                    Text(
                        text = stringResource(
                            R.string.exam_history_correct_answer,
                            correctAnswer
                                ?: stringResource(R.string.exam_history_content_unavailable),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailUnavailable(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.exam_history_error),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
