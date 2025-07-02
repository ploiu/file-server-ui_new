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
    val pageState: FolderUiState,
    val previews: BatchFilePreview,
)

class FolderPageViewModel(
    val folderService: FolderService,
    val previewService: PreviewService,
    val folderId: Long,
) : ViewModel() {
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

    /**
     * handles getting where the user wants to save the folder, downloading the folder from the server,
     * and writing the folder to the disk at the selected location
     * */
    fun downloadFolder(folder: FolderApi) {
        TODO()
    }

    fun updateFolder(folder: FolderApi) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.value.pageState
        if (currentState is FolderLoaded) {
            _state.update { it.copy(pageState = FolderLoading()) }
            val updateRes = folderService.updateFolder(folder.toUpdateFolder())
            updateRes.onSuccess {
                // TODO going round trip to the server just to update 1 folder might be wasteful.
                //  Might be better to just update the folder in our existing model without
                //  having to pull again...will need refresh logic though (ctrl + r)
                loadFolder()
            }.onFailure { msg ->
                _state.update { it.copy(pageState = FolderError(msg)) }
            }
        }
    }
}
