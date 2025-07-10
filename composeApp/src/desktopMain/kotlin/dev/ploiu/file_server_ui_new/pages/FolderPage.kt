package dev.ploiu.file_server_ui_new.pages

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
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.Dialog
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.components.TextDialog
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.viewModel.FolderError
import dev.ploiu.file_server_ui_new.viewModel.FolderLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel
import kotlinx.coroutines.runBlocking
import java.util.Objects

/**
 * used to control state for extra elements such as rename/delete dialogs and folder info dialogs
 */
private sealed interface FolderContextState

/**
 * used to control behavior for context menu actions on a folder
 */
private sealed interface FolderContextAction
private data class InfoFolderAction(val folder: FolderApi) : FolderContextAction

// actions that alter the state of this page directly
private class NoFolderAction : FolderContextAction, FolderContextState
private data class RenameFolderAction(val folder: FolderApi) : FolderContextAction, FolderContextState
private data class DeleteFolderAction(val folder: FolderApi) : FolderContextAction, FolderContextState

// This doesn't affect page state in the same way the others do
private data class DownloadFolderAction(val folder: FolderApi) : FolderContextAction

@Composable
fun FolderPage(
    view: FolderPageViewModel,
    /** used to force re-renders if data is updated externally (e.g. via a side sheet) */
    refreshKey: Int,
    onUpdate: () -> Unit,
    onFolderInfo: (FolderApi) -> Unit,
    onFolderNav: (FolderApi) -> Unit,
) {
    val (pageState, previews) = view.state.collectAsState().value
    var folderActionState: FolderContextState by remember {
        mutableStateOf(
            NoFolderAction()
        )
    }

    LaunchedEffect(Objects.hash(view.folderId, refreshKey)) {
        view.loadFolder()
    }

    /** used to trigger behavior when a context menu item is clicked */
    val onFolderContextAction: (FolderContextAction) -> Unit = {
        when (it) {
            is DownloadFolderAction -> {
                view.downloadFolder(it.folder)
                folderActionState = NoFolderAction()
            }

            is InfoFolderAction -> {
                onFolderInfo(it.folder)
                folderActionState = NoFolderAction()
            }

            is RenameFolderAction -> folderActionState = it
            is NoFolderAction -> folderActionState = it
            is DeleteFolderAction -> folderActionState = it
        }
    }

    when (pageState) {
        is FolderLoading -> Column {
            CircularProgressIndicator()
        }

        is FolderLoaded -> LoadedFolderList(
            pageState.folder, previews, onFolderNav, onFolderContextAction
        )

        is FolderError -> Dialog(
            title = "An Error Occurred",
            text = pageState.message,
            icon = Icons.Default.Error,
            iconColor = MaterialTheme.colorScheme.error,
        )
    }

    /* have to create a new variable here because folderActionState is delegated (basically a
    getter and not a raw value) */
    when (val action = folderActionState) {
        is RenameFolderAction -> {
            TextDialog(
                title = "Rename folder",
                defaultValue = action.folder.name,
                onCancel = { folderActionState = NoFolderAction() },
                onConfirm = {
                    val newFolder = action.folder.copy(name = it)
                    runBlocking {
                        folderActionState = NoFolderAction()
                        view.updateFolder(newFolder)
                        onUpdate()
                    }
                })
        }

        is DeleteFolderAction -> TODO("Delete Folder Functionality")
        is NoFolderAction -> Unit
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun DesktopFileEntry(file: FileApi, preview: ByteArray? = null) {
    TooltipArea(
        tooltip = {
            Surface(
                tonalElevation = 10.dp, color = MaterialTheme.colorScheme.tertiary
            ) { Text(file.name) }
        }) {
        FileEntry(file, preview)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DesktopFolderEntry(
    folder: FolderApi,
    onClick: (f: FolderApi) -> Unit,
    onContextAction: (FolderContextAction) -> Unit,
) {
    ContextMenuArea(items = {
        listOf(ContextMenuItem("Rename Folder") {
            onContextAction(RenameFolderAction(folder))
        }, ContextMenuItem("Delete Folder") {
            onContextAction(DeleteFolderAction(folder))
        }, ContextMenuItem("Download Folder") {
            onContextAction(DownloadFolderAction(folder))
        }, ContextMenuItem("Info") {
            onContextAction(InfoFolderAction(folder))
        })
    }) {
        TooltipArea(
            tooltip = {
                Surface(
                    tonalElevation = 10.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                ) { Text(folder.name) }
            }) {
            FolderEntry(folder, onClick = onClick)
        }
    }
}

@Composable
private fun LoadedFolderList(
    folder: FolderApi,
    previews: BatchFilePreview,
    onFolderNav: (FolderApi) -> Unit,
    onFolderContextAction: (FolderContextAction) -> Unit,
) {
    val children: List<Any> =
        folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }
    LazyVerticalGrid(
        // if this is changed, be sure to update SearchResultsPage.kt
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp
        ),
        columns = GridCells.Adaptive(150.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(children) { child -> // make all items have the same height
            when (child) {
                is FolderApi -> DesktopFolderEntry(
                    folder = child,
                    onClick = { onFolderNav(it) },
                    onContextAction = onFolderContextAction
                )

                is FileApi -> DesktopFileEntry(child, previews[child.id])
            }
        }
    }
}
