package com.metafoto.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metafoto.app.ui.components.ThemeModeDialog
import com.metafoto.app.ui.screens.ErrorScreen
import com.metafoto.app.ui.screens.HomeScreen
import com.metafoto.app.ui.screens.LoadingScreen
import com.metafoto.app.ui.screens.ResultScreen
import com.metafoto.app.ui.theme.MetaFotoTheme
import com.metafoto.app.ui.viewmodel.MainUiState
import com.metafoto.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            var showThemeDialog by rememberSaveable { mutableStateOf(false) }

            MetaFotoTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppContent(
                        uiState = uiState,
                        onPhotoSelected = viewModel::analyze,
                        onThemeClick = { showThemeDialog = true },
                        onRetry = viewModel::retry,
                        onChooseAnother = viewModel::goHome,
                        onBack = viewModel::goHome,
                    )
                }

                if (showThemeDialog) {
                    ThemeModeDialog(
                        currentMode = themeMode,
                        onModeSelected = viewModel::setThemeMode,
                        onDismiss = { showThemeDialog = false },
                    )
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = imageUriFrom(intent) ?: return
        viewModel.analyze(uri)
    }

    private fun imageUriFrom(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.takeIf { it.isImage(intent.type) }
            Intent.ACTION_SEND -> IntentCompat
                .getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.takeIf { it.isImage(intent.type) }
            else -> null
        }
    }

    private fun Uri.isImage(intentType: String?): Boolean {
        val type = intentType ?: runCatching { contentResolver.getType(this) }.getOrNull()
        return type == null || type.startsWith("image/")
    }
}

@Composable
private fun AppContent(
    uiState: MainUiState,
    onPhotoSelected: (Uri) -> Unit,
    onThemeClick: () -> Unit,
    onRetry: () -> Unit,
    onChooseAnother: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = uiState !is MainUiState.Home) {
        onBack()
    }

    when (uiState) {
        MainUiState.Home -> HomeScreen(
            onPhotoSelected = onPhotoSelected,
            onThemeClick = onThemeClick,
        )

        is MainUiState.Loading -> LoadingScreen()

        is MainUiState.Success -> ResultScreen(
            metadata = uiState.metadata,
            onBack = onBack,
            onChooseAnother = onChooseAnother,
            onThemeClick = onThemeClick,
        )

        is MainUiState.Error -> ErrorScreen(
            message = uiState.message,
            onRetry = onRetry,
            onChooseAnother = onChooseAnother,
            onBack = onBack,
        )
    }
}
