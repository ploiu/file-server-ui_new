package dev.ploiu.file_server_ui_new.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.service.FolderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update


enum class FolderLoadingState {
    LOADING,
    LOADED,
    ERROR
}

data class FolderState(val loadingState: FolderLoadingState, val folder: FolderApi?)

class FolderView(var folderService: FolderService, val folderId: Long): ViewModel() {
    private val _state = MutableStateFlow(FolderState(FolderLoadingState.LOADING, null))
    val state: StateFlow<FolderState> = _state.asStateFlow()

    fun getFolder() = viewModelScope.launch {
        try {
            _state.update { it -> it.copy(loadingState = FolderLoadingState.LOADED, folder = folderService.getFolder(folderId)) }
        } catch(e: Exception) {
            System.err.println(e)
        }
    }
}
