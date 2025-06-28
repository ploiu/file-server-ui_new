package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.PreviewService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class FolderRoute(val id: Long)

sealed interface FolderUiState
class FolderLoading : FolderUiState
data class FolderLoaded(val folder: FolderApi) : FolderUiState
data class FolderError(val message: String) : FolderUiState

data class FolderPageUiModel(
    val pageState: FolderUiState, val previews: BatchFilePreview
)

class FolderPageViewModel(val folderService: FolderService, val previewService: PreviewService, val folderId: Long) :
    ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(FolderPageUiModel(FolderLoading(), emptyMap()))
    val state = _state.asStateFlow()

    fun loadFolder() = viewModelScope.launch(Dispatchers.IO) {
        val folderRes = folderService.getFolder(folderId)
        folderRes.onSuccess { folder ->
            _state.update { it.copy(pageState = FolderLoaded(folder)) }
            previewService.getFolderPreview(folder).onSuccess { previews ->
                _state.update { it.copy(previews = previews) }
            }
        }.onFailure { error ->
            _state.update { it.copy(pageState = FolderError(error)) }
            log.error { "Failed to get folder information: $error" }
        }
    }
}
