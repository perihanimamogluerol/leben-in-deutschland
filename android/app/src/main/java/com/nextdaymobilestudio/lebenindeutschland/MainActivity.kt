package com.nextdaymobilestudio.lebenindeutschland

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.model.SupportedLanguage
import com.nextdaymobilestudio.lebenindeutschland.feature.languageselection.ui.LanguageSelectionRoute
import com.nextdaymobilestudio.lebenindeutschland.ui.theme.LebenInDeutschlandTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LebenInDeutschlandTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val confirmationTemplate = stringResource(
                    R.string.language_selection_temporary_confirmation,
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    LanguageSelectionRoute(
                        onContinue = { language ->
                            val requestedLocales = LocaleListCompat.forLanguageTags(language.code)
                            val currentLanguageTags = AppCompatDelegate
                                .getApplicationLocales()
                                .toLanguageTags()

                            if (currentLanguageTags == requestedLocales.toLanguageTags()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = confirmationTemplate.format(language.nativeName),
                                    )
                                }
                            } else {
                                applyApplicationLanguage(language)
                            }
                        },
                    )

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, bottom = 120.dp),
                    )
                }
            }
        }
    }

    private fun applyApplicationLanguage(language: SupportedLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code),
        )
    }
}
