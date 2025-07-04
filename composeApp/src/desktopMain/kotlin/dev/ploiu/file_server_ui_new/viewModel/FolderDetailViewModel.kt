package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.Tag
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import dev.ploiu.file_server_ui_new.service.FolderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FolderDetailUiState
class FolderDetailLoading : FolderDetailUiState
data class FolderDetailLoaded(val folder: FolderApi) : FolderDetailUiState
data class FolderDetailErrored(val message: String) : FolderDetailUiState

data class FolderDetailUiModel(
    val sheetState: FolderDetailUiState,
)

// TODO this has potential to be pulled into common code
class FolderDetailViewModel(
    val folderService: FolderService,
    val folderId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(FolderDetailUiModel(FolderDetailLoading()))
    val state = _state.asStateFlow()

    fun loadFolder() = viewModelScope.launch(Dispatchers.IO) {
        _state.update { it.copy(sheetState = FolderDetailLoading()) }
        val folderRes = folderService.getFolder(folderId)
        folderRes
            .onSuccess { folder -> _state.update { it.copy(sheetState = FolderDetailLoaded(folder)) } }
            .onFailure { msg -> _state.update { it.copy(sheetState = FolderDetailErrored(msg)) } }
    }

    fun renameFolder(newName: String) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.value.sheetState
        if (currentState is FolderDetailLoaded) {
            val toUpdate = currentState.folder.copy(name = newName).toUpdateFolder()
            updateFolder(toUpdate)
        }
    }

    fun deleteFolder() = viewModelScope.launch(Dispatchers.IO) {
        TODO()
    }

    fun updateTags(tags: Collection<Tag>) = viewModelScope.launch(Dispatchers.IO) {
        TODO()
    }

    private fun updateFolder(toUpdate: UpdateFolder) = viewModelScope.launch(Dispatchers.IO) {
        val updateFolderRes = folderService.updateFolder(toUpdate)
        updateFolderRes.onSuccess {
            loadFolder()
        }.onFailure { msg -> _state.update { it.copy(sheetState = FolderDetailErrored(msg)) } }
    }

}
