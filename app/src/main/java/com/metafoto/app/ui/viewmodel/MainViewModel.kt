package com.metafoto.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metafoto.app.data.repository.PhotoMetadataRepositoryImpl
import com.metafoto.app.domain.model.PhotoMetadata
import com.metafoto.app.domain.repository.PhotoMetadataRepository
import com.metafoto.app.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MainUiState {
    data object Home : MainUiState
    data class Loading(val uri: String) : MainUiState
    data class Success(val metadata: PhotoMetadata) : MainUiState
    data class Error(val message: String, val uri: String?) : MainUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoMetadataRepository = PhotoMetadataRepositoryImpl(application)
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, 0)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Home)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromStorage(preferences.getString(KEY_THEME, null)),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private var analysisJob: Job? = null
    private var lastUri: String? = null

    fun analyze(uri: Uri) = analyze(uri.toString())

    fun analyze(uri: String) {
        lastUri = uri
        analysisJob?.cancel()
        _uiState.value = MainUiState.Loading(uri)
        analysisJob = viewModelScope.launch {
            try {
                val result = repository.extract(uri)
                _uiState.value = result.fold(
                    onSuccess = { MainUiState.Success(it) },
                    onFailure = { MainUiState.Error(it.message ?: DEFAULT_ERROR, uri) },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = MainUiState.Error(
                    error.message ?: DEFAULT_ERROR,
                    uri,
                )
            }
        }
    }

    fun retry() {
        lastUri?.let(::analyze)
    }

    fun goHome() {
        analysisJob?.cancel()
        _uiState.value = MainUiState.Home
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.edit().putString(KEY_THEME, mode.storageValue).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "metafoto_settings"
        const val KEY_THEME = "theme_mode"
        const val DEFAULT_ERROR = "No se pudo analizar la foto."
    }
}
