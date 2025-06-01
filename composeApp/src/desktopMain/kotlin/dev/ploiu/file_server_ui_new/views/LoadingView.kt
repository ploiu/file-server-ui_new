package dev.ploiu.file_server_ui_new.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.ApiService
import dev.ploiu.file_server_ui_new.Global
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VersionCheckResult {
    LOADING,
    COMPATIBLE,
    INCOMPATIBLE
}

data class LoadingState(val checkResult: VersionCheckResult)

class LoadingView @Inject constructor (var apiService: ApiService) : ViewModel() {
    private val _state = MutableStateFlow(LoadingState(VersionCheckResult.LOADING))
    val state: StateFlow<LoadingState> = _state.asStateFlow()

    fun versionCheck() = viewModelScope.launch {
        val versionMatches = apiService.isCompatibleWithServer()
        if(versionMatches) {
            _state.value = LoadingState(VersionCheckResult.COMPATIBLE)
        } else {
            _state.value = LoadingState(VersionCheckResult.INCOMPATIBLE)
        }
    }
}

@Composable
fun LoadingScreen() {
    val modey by remember { Global.appComponent.loadingView()}
}
