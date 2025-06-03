package dev.ploiu.file_server_ui_new.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.service.FolderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class FolderRoute(val id: Long)

enum class FolderLoadingState {
    LOADING,
    LOADED,
    ERROR
}

data class FolderState(val loadingState: FolderLoadingState, val folder: FolderApi?)

// TODO look into moving this view model to the common code
class FolderView(var folderService: FolderService, val folderId: Long): ViewModel() {
    private val _state = MutableStateFlow(FolderState(FolderLoadingState.LOADING, null))
    val state: StateFlow<FolderState> = _state.asStateFlow()

    fun getFolder() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _state.update { it -> it.copy(loadingState = FolderLoadingState.LOADED, folder = folderService.getFolder(folderId)) }
        } catch(e: Exception) {
            System.err.println(e)
        }
    }
}

@Composable
fun FolderList(model: FolderView, onFolderNav: (f: FolderApi) -> Unit) {
    val (_, folder) = model.state.collectAsState().value

    LaunchedEffect(Unit) {
        model.getFolder()
    }
    if (folder != null) {
        val children: List<Any> =
            folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }
        LazyVerticalGrid(
            columns = GridCells.FixedSize(125.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(children) { child ->
                when (child) {
                    is FolderApi -> FolderEntry(child) { onFolderNav(it) }
                    is FileApi -> FileEntry(file = child)
                }
            }
        }
    }
}
