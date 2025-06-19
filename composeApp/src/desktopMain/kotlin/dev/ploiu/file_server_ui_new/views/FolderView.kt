package dev.ploiu.file_server_ui_new.views

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.components.Dialog
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.model.BatchFolderPreview
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

sealed interface FolderViewState
class LoadingFolderView : FolderViewState
class LoadedFolderView : FolderViewState
data class ErrorState(val message: String) : FolderViewState

data class FolderState(
    val pageState: FolderViewState,
    val folder: FolderApi?,
    val previews: BatchFolderPreview
)

// TODO look into moving this view model to the common code
class FolderView(var folderService: FolderService, var previewService: PreviewService, val folderId: Long) :
    ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(FolderState(LoadingFolderView(), null, emptyMap()))
    val state: StateFlow<FolderState> = _state.asStateFlow()

    fun getFolder() = viewModelScope.launch(Dispatchers.IO) {
        val folderRes = folderService.getFolder(folderId)
        folderRes
            .onSuccess { folder ->
                launch {
                    _state.update { it.copy(folder = folder, pageState = LoadedFolderView()) }
                }
                previewService.getFolderPreview(folder)
                    .onSuccess { previews ->
                        launch {
                            _state.update { it.copy(previews = previews) }
                        }
                    }
            }
            .onFailure { error ->
                launch {
                    _state.update { it.copy(pageState = ErrorState(error)) }
                }
                log.error { "Failed to get folder information: $error" }
            }
    }
}


@Composable
@OptIn(ExperimentalFoundationApi::class)
fun DesktopFileEntry(file: FileApi, preview: ByteArray?) {
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
fun DesktopFolderEntry(folder: FolderApi, onClick: (f: FolderApi) -> Unit) {
    ContextMenuArea(items = {
        listOf(
            ContextMenuItem("Rename Folder") {
                println("rename clicked")
            },
            ContextMenuItem("Delete Folder") {
                println("delete clicked")
            },
            ContextMenuItem("Download Folder") {
                println("download clicked")
            },
            ContextMenuItem("Info") {
                println("info clicked")
            }
        )
    }) {
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
}

@Composable
fun FolderList(model: FolderView, onFolderNav: (FolderApi) -> Unit) {
    val (pageState, folder, previews) = model.state.collectAsState().value

    LaunchedEffect(Unit) {
        model.getFolder()
    }

    when (pageState) {
        is LoadingFolderView -> Column {
            CircularProgressIndicator()
        }

        is LoadedFolderView -> LoadedFolderList(folder!!, previews, onFolderNav)
        is ErrorState -> Dialog(
            title = "An Error Occurred",
            text = pageState.message,
            icon = Icons.Default.Error,
            iconColor = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun LoadedFolderList(folder: FolderApi, previews: BatchFolderPreview, onFolderNav: (FolderApi) -> Unit) {
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
                is FolderApi -> DesktopFolderEntry(child) { onFolderNav(it) }
                is FileApi -> DesktopFileEntry(child, previews[child.id])
            }
        }
    }
}
