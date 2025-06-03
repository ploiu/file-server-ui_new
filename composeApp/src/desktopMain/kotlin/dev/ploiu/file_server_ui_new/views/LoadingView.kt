package dev.ploiu.file_server_ui_new.views

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.service.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

enum class VersionCheckResult {
    LOADING,
    COMPATIBLE,
    INCOMPATIBLE
}

data class LoadingState(val checkResult: VersionCheckResult)

class LoadingView constructor(var apiService: ApiService) : ViewModel() {
    private val _state = MutableStateFlow(LoadingState(VersionCheckResult.LOADING))
    val state: StateFlow<LoadingState> = _state.asStateFlow()

    fun versionCheck() = viewModelScope.launch {
        val versionMatches = apiService.isCompatibleWithServer()
        val compatibility = if (versionMatches) {
            VersionCheckResult.COMPATIBLE
        } else {
            VersionCheckResult.INCOMPATIBLE
        }
        _state.update { LoadingState(compatibility) }
    }
}

@Composable
fun LoadingScreen(model: LoadingView = koinViewModel()) {
    val state = model.state.collectAsState()

    LaunchedEffect(Unit) {
        model.versionCheck()
    }

    CircularProgressIndicator()

    Text(state.value.checkResult.name)
}
