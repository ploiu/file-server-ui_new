package dev.ploiu.file_server_ui_new.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import dev.ploiu.file_server_ui_new.service.PreviewService
import io.github.oshai.kotlinlogging.KotlinLogging
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

data class FolderState(val loadingState: FolderLoadingState, val folder: FolderApi?, val previews: Map<Long, ByteArray>)

// TODO look into moving this view model to the common code
class FolderView(var folderService: FolderService, var previewService: PreviewService, val folderId: Long) :
    ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(FolderState(FolderLoadingState.LOADING, null, emptyMap()))
    val state: StateFlow<FolderState> = _state.asStateFlow()

    fun getFolder() = viewModelScope.launch(Dispatchers.IO) {
        // TODO isn't there another way around this error handling with kotlin's special scope stuff? something something add it to Dispatchers.IO
        try {
            val folder = folderService.getFolder(folderId)
            launch {
                _state.update { it.copy(previews = getPreviews(folder)) }
            }
            _state.update { it -> it.copy(loadingState = FolderLoadingState.LOADED, folder = folder) }
        } catch(e: Exception) {
            log.error(e) { "Failed to get folder information" }
        }
    }

    private suspend fun getPreviews(folder: FolderApi): Map<Long, ByteArray> {
        val previews = previewService.getFolderPreview(folder)
        log.info { "previews downloaded!: " + previews.size }
        return previews
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FileEntryWithTooltip(file: FileApi, preview: ByteArray?) {
    TooltipArea(
        tooltip = {
            Surface(
                tonalElevation = 10.dp,
                color = MaterialTheme.colorScheme.tertiary
            ) { Text(file.name) }
        }) {
        FileEntry(file, preview)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FolderEntryWithTooltip(folder: FolderApi, onClick: (f: FolderApi) -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(
                tonalElevation = 10.dp,
                color = MaterialTheme.colorScheme.tertiary
            ) { Text(folder.name) }
        }) {
        FolderEntry(folder, onClick)
    }
}

@Composable
fun FolderList(model: FolderView, onFolderNav: (f: FolderApi) -> Unit) {
    val (_, folder, previews) = model.state.collectAsState().value

    LaunchedEffect(Unit) {
        model.getFolder()
    }
    if (folder != null) {
        val children: List<Any> =
            folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }
        LazyVerticalGrid(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp
            ),
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(children) { child ->
                // make all items have the same height
                when (child) {
                    is FolderApi -> FolderEntryWithTooltip(child) { onFolderNav(it) }
                    is FileApi -> FileEntryWithTooltip(child, previews[child.id])
                }
            }
        }
    }
}
