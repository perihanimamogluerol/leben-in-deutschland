package com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ui.ContentBootstrapScreen
import com.nextdaymobilestudio.lebenindeutschland.feature.contentbootstrap.ui.ContentBootstrapTestTags
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentBootstrapScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsProgress() {
        setScreen(ContentBootstrapUiState.Loading)

        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.LOADING)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.RETRY)
            .assertDoesNotExist()
    }

    @Test
    fun readyStateCanShowBackgroundRefreshWithoutBlockingContent() {
        setScreen(
            ContentBootstrapUiState.Ready(
                releaseId = "release-3",
                questionCount = 460,
                isRefreshing = true,
            ),
        )

        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.READY)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.REFRESHING)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.ERROR)
            .assertDoesNotExist()
    }

    @Test
    fun readyStateShowsNonBlockingWarningAfterRefreshFailure() {
        setScreen(
            ContentBootstrapUiState.Ready(
                releaseId = "release-3",
                questionCount = 460,
                isRefreshing = false,
                refreshFailed = true,
            ),
        )

        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.READY)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.REFRESH_WARNING)
            .assertIsDisplayed()
    }

    @Test
    fun firstInstallErrorOffersRetry() {
        var retried = false
        setScreen(
            state = ContentBootstrapUiState.Error,
            onRetry = { retried = true },
        )

        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.ERROR)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(ContentBootstrapTestTags.RETRY)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(retried)
        }
    }

    private fun setScreen(
        state: ContentBootstrapUiState,
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            LebenInDeutschlandTheme {
                ContentBootstrapScreen(
                    uiState = state,
                    onRetry = onRetry,
                )
            }
        }
    }
}
