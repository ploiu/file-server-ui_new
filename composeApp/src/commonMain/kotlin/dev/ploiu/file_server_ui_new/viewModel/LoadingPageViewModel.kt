package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
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

    /** compatibility result succeeded - navigating to next page */
    NAVIGATING
}

data class LoadingPageUiModel(
    val pageState: LoadingUiState,
    val checkResult: ServerCompatibilityResult?,
)

class LoadingPageViewModel(var apiService: ApiService, modalController: ModalController) :
    ViewModelWithModal(modalController) {
    private val _state = MutableStateFlow(LoadingPageUiModel(LoadingUiState.LOADING, null))
    val state: StateFlow<LoadingPageUiModel> = _state.asStateFlow()

    fun versionCheck() = viewModelScope.launch {
        val compatibility = try {
            apiService.isCompatibleWithServer()
        } catch (e: Exception) {
            ErrorResult(e)
        }
        if (compatibility is CompatibleResult) {
            _state.update { LoadingPageUiModel(LoadingUiState.NAVIGATING, compatibility) }
        } else {
            when (compatibility) {
                is IncompatibleResult -> {
                    ErrorModal.open(
                        ErrorModalProps(
                            title = "Incompatible server version",
                            text = "The server is on ${compatibility.serverVersion}, but this client only supports ${compatibility.compatibleVersion}",
                            icon = Icons.Default.Info,
                            iconColorProvider = @Composable { MaterialTheme.colorScheme.secondary },
                            onClose = {},
                        ),
                    )
                }

                is ErrorResult -> {
                    ErrorModal.open(
                        ErrorModalProps(
                            title = "An error occurred",
                            text = compatibility.error.message,
                            icon = Icons.Default.Error,
                            iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                            onClose = {},
                        ),
                    )
                }

                else -> throw UnsupportedOperationException("Invalid state reached when rendering the loading page!: $compatibility")
            }
        }
    }
}
