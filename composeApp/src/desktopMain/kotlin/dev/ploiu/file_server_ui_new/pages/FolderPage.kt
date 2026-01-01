package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.CustomDataFlavors
import dev.ploiu.file_server_ui_new.FolderChildSelection
import dev.ploiu.file_server_ui_new.MouseInsideWindow
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.components.KindaLazyVerticalGrid
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderChild
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.floor
import kotlin.math.max

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
    viewModel: FolderPageViewModel,
    /** used to force re-renders if data is updated externally (e.g. via a side sheet) */
    refreshKey: String,
    /** used to visually change the page when drag and drop is active */
    isDragging: Boolean,
    mousePosition: MouseInsideWindow,
    /** used to tell other components to refresh their data when this one updates something internally */
    onUpdate: () -> Unit,
    onFolderInfo: (FolderApi) -> Unit,
    onFileInfo: (FileApi) -> Unit,
    onFolderNav: (FolderApi) -> Unit,
) {
    val (pageState, previews, updateKey, errorMessage) = viewModel.state.collectAsState().value
    // needed as a glue storage object for when opening the system dialog to save a file / folder
    var downloadingType by remember { mutableStateOf<DownloadingSelection>(NoDownloadSelection) }
    val directoryPicker = rememberFileSaverLauncher { selectedFile ->
        if (selectedFile == null) {
            return@rememberFileSaverLauncher
        }
        val selection = downloadingType
        if (selection is DownloadingFolder) {
            viewModel.downloadFolder(selection.folder, selectedFile)
        } else if (selection is DownloadingFile) {
            viewModel.downloadFile(selection.file, selectedFile)
        }
        downloadingType = NoDownloadSelection
    }
    LaunchedEffect(Objects.hash(viewModel.folderId, refreshKey)) {
        viewModel.loadFolder()
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

            is RenameFolderAction -> viewModel.openRenameFolderModal(it.folder)

            is DeleteFolderAction -> viewModel.openDeleteFolderModal(it.folder)
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

            is RenameFileAction -> viewModel.openRenameFileModal(it.file)
            is DeleteFileAction -> viewModel.openDeleteFileModal(it.file)
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
                mousePosition = mousePosition,
                onFolderNav = onFolderNav,
                onFolderContextAction = onFolderContextAction,
                onFileContextAction = onFileContextAction,
                onFolderChildDropped = { targetFolder, child ->
                    if (child is FileApi) {
                        viewModel.updateFile(child.copy(folderId = targetFolder.id))
                    } else if (child is FolderApi) {
                        viewModel.updateFolder(child.copy(parentId = targetFolder.id))

                    }
                },
            )
            if (errorMessage != null) {
                // we have to use weird stuff like this because we're not using the Scaffold for the desktop app
                // TODO not important now, but make this animate in/out. Make root element be Box for this component.
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-16).dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    dismissAction = {
                        IconButton(onClick = { viewModel.clearMessage() }) {
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
    modifier: Modifier = Modifier,
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
                modifier = Modifier.dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dropTarget,
                ) then modifier,
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
    /** is any drag and drop action being performed */
    isDragging: Boolean,
    /** the position of the mouse within the window */
    mousePosition: MouseInsideWindow,
    /** callback when the user wants to navigate to a folder */
    onFolderNav: (FolderApi) -> Unit,
    onFolderContextAction: (FolderContextAction) -> Unit,
    onFileContextAction: (FileContextAction) -> Unit,
    /** when a folder or a file is dragged and dropped to another folder */
    onFolderChildDropped: (FolderApi, FolderChild) -> Unit,
) {
    val folders = folder.folders.sortedBy { it.name }
    val files = folder.files.sortedByDescending { it.dateCreated }
    val children: List<FolderChild> =
        folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var isDragScrolling by remember { mutableStateOf(false) }
    val contentPadding = PaddingValues(
        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
    )
    val columns = GridCells.Adaptive(150.dp)

    /*
        Scrolling rules:
        1. if we're dragging and the mouse is close enough to the top of the screen, scroll to the very top
        2. if we're dragging and the mouse is close enough to the bottom, scroll to the very bottom
        3. if we're not dragging or the mouse is out the window, don't scroll
        4. if we stop dragging, we stop scrolling
        5. don't try and scroll if we're already scrolling, unless it's in the opposite direction
     */
    LaunchedEffect(mousePosition, isDragging) {
        scope.launch {
            if (isDragging && mousePosition != MouseInsideWindow.Invalid) {
                if (!isDragScrolling) {
                    if (mousePosition.percentFromTop <= .4f) {
                        isDragScrolling = true
                        gridState.animateScrollToItem(0)
                    } else if (mousePosition.percentFromBottom <= .1f) {
                        isDragScrolling = true
                        gridState.animateScrollToItem(children.size - 1)
                    }
                }
            } else {
                isDragScrolling = false
                gridState.stopScroll()
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // the permanent items need to be sized and spaced the same as the lazy ones
        // TODO cleanup and try to move within KindaLazyVerticalGrid
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val horizontalSpacing = with(density) { Arrangement.spacedBy(16.dp).spacing.toPx() }
        val paddingInPixels = with(density) {
            contentPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() + contentPadding
                .calculateRightPadding(LayoutDirection.Ltr)
                .toPx()
        }
        val cellWidthInPixels = with(density) {
            val minInPixels = 150.dp.toPx()
            val bonusSize = max(floor((maxWidthPx + horizontalSpacing) / (minInPixels + horizontalSpacing)).toInt(), 1)
            (maxWidthPx - paddingInPixels - horizontalSpacing * (bonusSize - 1)) / bonusSize
        }
        val cellWidth = with(density) { cellWidthInPixels.toDp() }
        KindaLazyVerticalGrid(
            // if this is changed, be sure to update SearchResultsPage.kt
            contentPadding = contentPadding,
            // TODO change to minColumnSize and hard code adaptive internally
            columns = columns,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            lazyState = gridState,
        ) {
            permanentItems = {
                for (item in folders) {
                    DesktopFolderEntry(
                        folder = item,
                        modifier = Modifier.width(cellWidth),
                        onClick = { onFolderNav(it) },
                        onContextAction = onFolderContextAction,
                        onDrop = {
                            if (it.awtTransferable.isDataFlavorSupported(CustomDataFlavors.FOLDER_CHILD)) {
                                val child =
                                    it.awtTransferable.getTransferData(CustomDataFlavors.FOLDER_CHILD) as FolderChild
                                onFolderChildDropped(item, child)
                            }
                        },
                    )
                }
            }
            lazyItems = {
                items(files) { item ->
                    DesktopFileEntry(
                        file = item,
                        preview = previews[item.id],
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
                                    transferable = DragAndDropTransferable(FolderChildSelection(item)),
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
}
