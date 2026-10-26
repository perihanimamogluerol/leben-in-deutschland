package com.nextdaymobilestudio.lebenindeutschland.feature.home.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextdaymobilestudio.lebenindeutschland.R
import com.nextdaymobilestudio.lebenindeutschland.feature.home.HomeProgressUiState
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme

@Composable
fun HomeScreen(
    languageName: String,
    stateName: String,
    studyProgress: HomeProgressUiState = HomeProgressUiState(),
    hasActiveMockExam: Boolean = false,
    hasCompletedMockExam: Boolean = false,
    onStartStudy: () -> Unit,
    onReviewWrongAnswers: () -> Unit = {},
    onOpenMockExam: () -> Unit = {},
    onOpenExamHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            StartStudySection(
                progress = studyProgress,
                onStartStudy = onStartStudy,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .testTag(HomeTestTags.CONTENT_LIST),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 32.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                HomeHeader(onOpenSettings = onOpenSettings)
            }

            if (studyProgress.totalCount > 0) {
                item {
                    StudyProgressSummary(progress = studyProgress)
                }
            }

            if (studyProgress.answeredCount > 0) {
                item {
                    WrongAnswersSummary(
                        progress = studyProgress,
                        onReviewWrongAnswers = onReviewWrongAnswers,
                    )
                }
            }

            item {
                MockExamSummary(
                    hasActiveExam = hasActiveMockExam,
                    hasCompletedExam = hasCompletedMockExam,
                    onOpenMockExam = onOpenMockExam,
                    onOpenExamHistory = onOpenExamHistory,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_your_selection),
                    modifier = Modifier.semantics { heading() },
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                SelectionSummary(
                    label = stringResource(R.string.home_language),
                    value = languageName,
                    testTag = HomeTestTags.LANGUAGE,
                )
            }

            item {
                SelectionSummary(
                    label = stringResource(R.string.home_federal_state),
                    value = stateName,
                    testTag = HomeTestTags.STATE,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.home_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        FilledTonalIconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(52.dp)
                .testTag(HomeTestTags.OPEN_SETTINGS),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
            )
        }
    }
}

@Composable
private fun MockExamSummary(
    hasActiveExam: Boolean,
    hasCompletedExam: Boolean,
    onOpenMockExam: () -> Unit,
    onOpenExamHistory: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.MOCK_EXAM),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_mock_exam_title),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.home_mock_exam_description),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onOpenMockExam,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(HomeTestTags.OPEN_MOCK_EXAM),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        when {
                            hasActiveExam -> R.string.home_mock_exam_resume
                            hasCompletedExam -> R.string.home_mock_exam_result
                            else -> R.string.home_mock_exam_start
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            FilledTonalButton(
                onClick = onOpenExamHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag(HomeTestTags.OPEN_EXAM_HISTORY),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_exam_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun WrongAnswersSummary(
    progress: HomeProgressUiState,
    onReviewWrongAnswers: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.WRONG_ANSWERS),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (progress.hasWrongAnswers) {
                        Icons.Filled.Refresh
                    } else {
                        Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.home_wrong_answers_title),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (progress.hasWrongAnswers) {
                    stringResource(R.string.home_wrong_answers_count, progress.wrongCount)
                } else {
                    stringResource(R.string.home_wrong_answers_empty)
                },
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (progress.hasWrongAnswers) {
                FilledTonalButton(
                    onClick = onReviewWrongAnswers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(HomeTestTags.REVIEW_WRONG_ANSWERS),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.home_review_wrong_answers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyProgressSummary(progress: HomeProgressUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.STUDY_PROGRESS),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_progress_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.home_progress_answered,
                    progress.answeredCount,
                    progress.totalCount,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 10.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            if (progress.answeredCount > 0) {
                Text(
                    text = stringResource(
                        R.string.home_progress_correct,
                        progress.correctCount,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SelectionSummary(
    label: String,
    value: String,
    testTag: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StartStudySection(
    progress: HomeProgressUiState,
    onStartStudy: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onStartStudy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .testTag(HomeTestTags.START_STUDY),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        when {
                            progress.isComplete -> R.string.home_study_again
                            progress.answeredCount > 0 -> R.string.home_continue_studying
                            else -> R.string.home_start_studying
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.home_start_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    LebenInDeutschlandTheme {
        HomeScreen(
            languageName = "Türkçe",
            stateName = "Hessen",
            onStartStudy = {},
            onReviewWrongAnswers = {},
        )
    }
}
