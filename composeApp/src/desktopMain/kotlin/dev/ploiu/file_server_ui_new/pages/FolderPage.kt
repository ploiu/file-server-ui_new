package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.components.dialog.Dialog
import dev.ploiu.file_server_ui_new.components.dialog.TextDialog
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.viewModel.FolderPageError
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.runBlocking
import java.util.*

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

// used when a modal should show to confirm replacing an existing download
private data class ConfirmReplaceDownloadFolder(val folder: FolderApi, val directory: PlatformFile) : FolderContextState

// This doesn't affect page state in the same way the others do
private data class DownloadFolderAction(val folder: FolderApi) : FolderContextAction, FolderContextState

/** used to control state for extra elements such as rename/delete dialogs and file info dialogs */
private sealed interface FileContextState

// TODO move these to a place that's not specifically for the folder page, as it's used on search results too
/** used to control behavior for context menu actions on a file */
sealed interface FileContextAction
data class InfoFileAction(val file: FileApi) : FileContextAction

// "blank" action that acts as a placeholder - no action to be done
class NoFileAction : FileContextAction, FileContextState
data class RenameFileAction(val file: FileApi) : FileContextAction, FileContextState
data class DeleteFileAction(val file: FileApi) : FileContextAction, FileContextState

// used when a modal should show to confirm replacing an existing download
data class ConfirmReplaceDownloadFile(val file: FileApi, val directory: PlatformFile) : FileContextState

// This doesn't affect page state in the same way the others do
data class DownloadFileAction(val file: FileApi) : FileContextAction, FileContextState

@Composable
fun FolderPage(
    view: FolderPageViewModel,
    /** used to force re-renders if data is updated externally (e.g. via a side sheet) */
    refreshKey: String,
    onUpdate: () -> Unit,
    onFolderInfo: (FolderApi) -> Unit,
    onFileInfo: (FileApi) -> Unit,
    onFolderNav: (FolderApi) -> Unit,
) {
    val (pageState, previews, errorMessage) = view.state.collectAsState().value
    var folderActionState: FolderContextState by remember {
        mutableStateOf(
            NoFolderAction(),
        )
    }
    var fileActionState: FileContextState by remember {
        mutableStateOf(
            NoFileAction(),
        )
    }
    val directoryPicker = rememberFileSaverLauncher { selectedFile ->
        val actionState = folderActionState
        if (selectedFile != null && actionState is DownloadFolderAction) {
            view.downloadFolder(actionState.folder, selectedFile)
            folderActionState = NoFolderAction()
        }
        val fileAction = fileActionState
        if (selectedFile != null && fileAction is DownloadFileAction) {
            view.downloadFile(fileAction.file, selectedFile)
            fileActionState = NoFileAction()
        }
    }

    LaunchedEffect(Objects.hash(view.folderId, refreshKey)) {
        view.loadFolder()
    }

    /** used to trigger behavior when a context menu item is clicked */
    val onFolderContextAction: (FolderContextAction) -> Unit = {
        when (it) {
            is DownloadFolderAction -> {
                folderActionState = DownloadFolderAction(it.folder)
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

    /** used to trigger behavior when a context menu item is clicked */
    val onFileContextAction: (FileContextAction) -> Unit = {
        when (it) {
            is DownloadFileAction -> {
                fileActionState = DownloadFileAction(it.file)
            }

            is InfoFileAction -> {
                onFileInfo(it.file)
                fileActionState = NoFileAction()
            }

            is RenameFileAction -> fileActionState = it
            is NoFileAction -> fileActionState = it
            is DeleteFileAction -> fileActionState = it
        }
    }

    when (pageState) {
        is FolderPageLoading -> Column {
            CircularProgressIndicator()
        }

        is FolderPageLoaded -> Box {
            LoadedFolderList(
                pageState.folder, previews, onFolderNav, onFolderContextAction, onFileContextAction,
            )
            if (errorMessage != null) {
                // we have to use weird stuff like this because we're not using the Scaffold for the desktop app
                // TODO not important now, but make this animate in/out. Make root element be Box for this component.
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-16).dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    dismissAction = {
                        IconButton(onClick = { view.clearMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss error message")
                        }
                    },
                ) {
                    Text(text = errorMessage, modifier = Modifier.testTag("folderErrorMessage"))
                }
            }
        }

        is FolderPageError -> Dialog(
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
            TODO("register with ModalController")
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
                },
            )
        }

        is DownloadFolderAction -> {
            directoryPicker.launch(action.folder.name, "tar")
        }

        is DeleteFolderAction -> {
            TODO("register with modal controller")
            TextDialog(
                title = "Delete folder",
                bodyText = "Are you sure you want to delete this folder? Type the name to confirm",
                confirmText = "Delete",
                onConfirm = {
                    if (it == action.folder.name) {
                        folderActionState = NoFolderAction()
                        view.deleteFolder(action.folder)
                    }
                },
                onCancel = { folderActionState = NoFolderAction() },
            )
        }

        is ConfirmReplaceDownloadFolder -> {
            TODO("register with modal controller")
            Dialog(
                title = "File already exists",
                text = "A file named ${action.folder.name}.tar already exists in the selected directory. Do you want to replace it?",
                icon = Icons.Default.Error,
                iconColor = MaterialTheme.colorScheme.error,
                confirmText = "Replace",
                dismissText = "Cancel",
                onDismissRequest = { folderActionState = NoFolderAction() },
                onConfirm = {
                    view.downloadFolder(action.folder, action.directory)
                    folderActionState = NoFolderAction()
                },
            )
        }

        is NoFolderAction -> Unit
    }

    /* handle file actions */
    when (val action = fileActionState) {
        is RenameFileAction -> {
            TODO("register with modal controller")
            TextDialog(
                title = "Rename file",
                defaultValue = action.file.name,
                onCancel = { fileActionState = NoFileAction() },
                onConfirm = {
                    val newFile = action.file.copy(name = it)
                    runBlocking {
                        fileActionState = NoFileAction()
                        view.updateFile(newFile)
                        onUpdate()
                    }
                },
            )
        }

        is DownloadFileAction -> {
            directoryPicker.launch(action.file.nameWithoutExtension, action.file.extension)
        }

        is DeleteFileAction -> {
            TODO("register with modal controller")
            Dialog(
                title = "Delete file",
                text = "Are you sure you want to delete this file?",
                confirmText = "Delete",
                onConfirm = {
                    fileActionState = NoFileAction()
                    view.deleteFile(action.file)
                },
                icon = Icons.Default.Warning,
                onDismissRequest = { fileActionState = NoFileAction() },
            )
        }

        is ConfirmReplaceDownloadFile -> {
            TODO("register with modal controller")
            Dialog(
                title = "File already exists",
                text = "A file named ${action.file.name} already exists in the selected directory. Do you want to replace it?",
                icon = Icons.Default.Error,
                iconColor = MaterialTheme.colorScheme.error,
                confirmText = "Replace",
                dismissText = "Cancel",
                onDismissRequest = { fileActionState = NoFileAction() },
                onConfirm = {
                    view.downloadFile(action.file, action.directory)
                    fileActionState = NoFileAction()
                },
            )
        }

        is NoFileAction -> Unit
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun DesktopFileEntry(
    file: FileApi,
    preview: ByteArray? = null,
    onClick: (f: FileApi) -> Unit,
    onContextAction: (FileContextAction) -> Unit,
) {
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Save as...") {
                    onContextAction(DownloadFileAction(file))
                },
                ContextMenuItem("Rename File") {
                    onContextAction(RenameFileAction(file))
                },
                ContextMenuItem("Delete File") {
                    onContextAction(DeleteFileAction(file))
                },
                ContextMenuItem("Info") {
                    onContextAction(InfoFileAction(file))
                },
            )
        },
    ) {
        TooltipArea(
            tooltip = {
                Surface(
                    tonalElevation = 10.dp, color = MaterialTheme.colorScheme.tertiary,
                ) { Text(file.name) }
            },
        ) {
            FileEntry(file, preview)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DesktopFolderEntry(
    folder: FolderApi,
    onClick: (f: FolderApi) -> Unit,
    onContextAction: (FolderContextAction) -> Unit,
) {
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Rename Folder") {
                    onContextAction(RenameFolderAction(folder))
                },
                ContextMenuItem("Delete Folder") {
                    onContextAction(DeleteFolderAction(folder))
                },
                ContextMenuItem("Download Folder") {
                    onContextAction(DownloadFolderAction(folder))
                },
                ContextMenuItem("Info") {
                    onContextAction(InfoFolderAction(folder))
                },
            )
        },
    ) {
        TooltipArea(
            tooltip = {
                Surface(
                    tonalElevation = 10.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = MaterialTheme.shapes.small,
                ) { Text(folder.name, modifier = Modifier.padding(3.dp)) }
            },
        ) {
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
    onFileContextAction: (FileContextAction) -> Unit,
) {
    val children: List<Any> =
        folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }
    LazyVerticalGrid(
        // if this is changed, be sure to update SearchResultsPage.kt
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
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
                    onContextAction = onFolderContextAction,
                )

                is FileApi -> DesktopFileEntry(
                    file = child,
                    preview = previews[child.id],
                    onClick = { TODO("file single / double click not implemented") },
                    onContextAction = onFileContextAction,
                )
            }
        }
    }
}
