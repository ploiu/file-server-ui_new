package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Move
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.CustomDataFlavors
import dev.ploiu.file_server_ui_new.FolderChildSelection
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderChild
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import java.util.*

private sealed interface DownloadingSelection
private data class DownloadingFolder(val folder: FolderApi) : DownloadingSelection
private data class DownloadingFile(val file: FileApi) : DownloadingSelection
private object NoDownloadSelection : DownloadingSelection

/**
 * used to control behavior for context menu actions on a folder
 */
private sealed interface FolderContextAction
private data class InfoFolderAction(val folder: FolderApi) : FolderContextAction
private data class RenameFolderAction(val folder: FolderApi) : FolderContextAction
private data class DeleteFolderAction(val folder: FolderApi) : FolderContextAction
private data class DownloadFolderAction(val folder: FolderApi) : FolderContextAction

// TODO move these to a place that's not specifically for the folder page, as it's used on search results too
/** used to control behavior for context menu actions on a file */
sealed interface FileContextAction
data class InfoFileAction(val file: FileApi) : FileContextAction
data class RenameFileAction(val file: FileApi) : FileContextAction
data class DeleteFileAction(val file: FileApi) : FileContextAction
data class DownloadFileAction(val file: FileApi) : FileContextAction

@Composable
fun FolderPage(
    view: FolderPageViewModel,
    /** used to force re-renders if data is updated externally (e.g. via a side sheet) */
    refreshKey: String,
    /** used to visually change the page when drag and drop is active */
    isDragging: Boolean,
    /** used to tell other components to refresh their data when this one updates something internally */
    onUpdate: () -> Unit,
    onFolderInfo: (FolderApi) -> Unit,
    onFileInfo: (FileApi) -> Unit,
    onFolderNav: (FolderApi) -> Unit,
) {
    val (pageState, previews, updateKey, errorMessage) = view.state.collectAsState().value
    // needed as a glue storage object for when opening the system dialog to save a file / folder
    var downloadingType by remember { mutableStateOf<DownloadingSelection>(NoDownloadSelection) }
    val directoryPicker = rememberFileSaverLauncher { selectedFile ->
        if (selectedFile == null) {
            return@rememberFileSaverLauncher
        }
        val selection = downloadingType
        if (selection is DownloadingFolder) {
            view.downloadFolder(selection.folder, selectedFile)
        } else if (selection is DownloadingFile) {
            view.downloadFile(selection.file, selectedFile)
        }
        downloadingType = NoDownloadSelection
    }
    LaunchedEffect(Objects.hash(view.folderId, refreshKey)) {
        view.loadFolder()
    }

    // every time our controller makes an update to data, we trigger the app to refresh its views
    LaunchedEffect(updateKey) {
        if (updateKey > 0) {
            onUpdate()
        }
    }

    /** used to trigger behavior when a context menu item is clicked */
    val onFolderContextAction: (FolderContextAction) -> Unit = {
        when (it) {
            is DownloadFolderAction -> {
                downloadingType = DownloadingFolder(it.folder)
                directoryPicker.launch(it.folder.name, "tar")
            }

            is InfoFolderAction -> onFolderInfo(it.folder)

            is RenameFolderAction -> view.openRenameFolderModal(it.folder)

            is DeleteFolderAction -> view.openDeleteFolderModal(it.folder)
        }
    }

    /** used to trigger behavior when a context menu item is clicked */
    val onFileContextAction: (FileContextAction) -> Unit = {
        when (it) {
            is DownloadFileAction -> {
                downloadingType = DownloadingFile(it.file)
                directoryPicker.launch(it.file.nameWithoutExtension, it.file.extension)
            }

            is InfoFileAction -> onFileInfo(it.file)

            is RenameFileAction -> view.openRenameFileModal(it.file)
            is DeleteFileAction -> view.openDeleteFileModal(it.file)
        }
    }

    when (pageState) {
        is FolderPageLoading -> Column {
            CircularProgressIndicator()
        }

        is FolderPageLoaded -> Box {
            LoadedFolderList(
                folder = pageState.folder,
                previews = previews,
                isDragging = isDragging,
                onFolderNav = onFolderNav,
                onFolderContextAction = onFolderContextAction,
                onFileContextAction = onFileContextAction,
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
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun DesktopFileEntry(
    file: FileApi,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    preview: ByteArray? = null,
    onClick: (f: FileApi) -> Unit = { TODO("normal file click") },
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
            FileEntry(file, modifier = modifier, preview = preview, imageModifier = imageModifier)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DesktopFolderEntry(
    folder: FolderApi,
    onClick: (f: FolderApi) -> Unit,
    onContextAction: (FolderContextAction) -> Unit,
    onDrop: (DragAndDropEvent) -> Unit,
) {
    var isDraggingOver by remember { mutableStateOf(false) }

    val dropTarget = remember {
        object : DragAndDropTarget {

            override fun onEntered(event: DragAndDropEvent) {
                super.onEntered(event)
                isDraggingOver = true
            }

            override fun onExited(event: DragAndDropEvent) {
                super.onExited(event)
                isDraggingOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDrop(event)
                return true
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                isDraggingOver = false
            }
        }
    }

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
            FolderEntry(
                folder,
                onClick = onClick,
                modifier = Modifier.dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget),
                surfaceColor = if (isDraggingOver) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoadedFolderList(
    folder: FolderApi,
    previews: BatchFilePreview,
    isDragging: Boolean,
    onFolderNav: (FolderApi) -> Unit,
    onFolderContextAction: (FolderContextAction) -> Unit,
    onFileContextAction: (FileContextAction) -> Unit,
) {
    val children: List<FolderChild> =
        folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }
    LazyVerticalGrid(
        // if this is changed, be sure to update SearchResultsPage.kt
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
        ),
        columns = GridCells.Adaptive(150.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // TODO look into Modifier.pointerInput(Unit) with detectDragGestures
    ) {
        items(children) { child -> // make all items have the same height
            when (child) {
                is FolderApi -> DesktopFolderEntry(
                    folder = child,
                    onClick = { onFolderNav(it) },
                    onContextAction = onFolderContextAction,
                    onDrop = {
                        if (it.awtTransferable.isDataFlavorSupported(CustomDataFlavors.FOLDER_CHILD)) {
                            val child = it.awtTransferable.getTransferData(CustomDataFlavors.FOLDER_CHILD)
                            if (child is FileApi) {
                                TODO("move file to new folder")
                            } else if (child is FolderApi) {
                                TODO("move folder to new folder")
                            }
                        }
                    },
                )

                is FileApi -> DesktopFileEntry(
                    file = child,
                    preview = previews[child.id],
                    onClick = { TODO("file single / double click not implemented") },
                    onContextAction = onFileContextAction,
                    imageModifier = if (isDragging) {
                        Modifier.alpha(.25f)
                    } else {
                        Modifier
                    },
                    modifier = Modifier.dragAndDropSource(
                        drawDragDecoration = {
                            // TODO this doesn't work on linux, and neither does the example on jetbrains' own website. So skipping this for now
                            /* val bitmap = previews[child.id]?.toImageBitmap() ?: determineBitmapIcon(child)
                            drawImage(
                                image = bitmap,
                                dstSize = IntSize(width = bitmap.width, height = bitmap.height),
                                dstOffset = IntOffset(0, 0)
                            ) */
                        },
                        transferData = { offset ->
                            DragAndDropTransferData(
                                transferable = DragAndDropTransferable(FolderChildSelection(child)),
                                supportedActions = listOf(Move),
                                dragDecorationOffset = offset,
                                onTransferCompleted = {
                                    // TODO move the file to the folder
                                },
                            )
                        },
                    ),
                )
            }
        }
    }
}
