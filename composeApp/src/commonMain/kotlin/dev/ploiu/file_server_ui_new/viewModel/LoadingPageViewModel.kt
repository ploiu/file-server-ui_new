package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
class LoadingRoute

enum class LoadingUiState {
    /** running compatibility check */
    LOADING,

    /** either an exception is thrown or the server is incompatible */
    SHOWING_MODAL,

    /** compatibility result succeeded - navigating to next page */
    NAVIGATING
}

data class LoadingPageUiModel(val pageState: LoadingUiState, val checkResult: ServerCompatibilityResult?)

class LoadingPageViewModel(var apiService: ApiService) : ViewModel() {
    private val _state = MutableStateFlow(LoadingPageUiModel(LoadingUiState.LOADING, null))
    val state: StateFlow<LoadingPageUiModel> = _state.asStateFlow()

    fun versionCheck() = viewModelScope.launch {
        val compatibility = try {
            apiService.isCompatibleWithServer()
        } catch (e: Exception) {
            ErrorResult(e)
        }
        _state.update {
            val pageState = when (compatibility) {
                is CompatibleResult -> LoadingUiState.NAVIGATING
                is IncompatibleResult -> LoadingUiState.SHOWING_MODAL
                is ErrorResult -> LoadingUiState.SHOWING_MODAL
            }
            LoadingPageUiModel(pageState, compatibility)
        }
        compatibility
    }
}
