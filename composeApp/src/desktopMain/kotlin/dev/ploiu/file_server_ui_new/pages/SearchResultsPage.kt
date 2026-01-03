package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Move
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.FolderChildSelection
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsError
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsLoaded
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsLoading
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsPageViewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import java.util.Objects

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchResultsPage(
    viewModel: SearchResultsPageViewModel,
    /** used to force re-renders if data is updated externally (e.g. via a side sheet) */
    refreshKey: String,
    /** used to visually change the page when drag and drop is active */
    isDragging: Boolean,
    onFileClick: (FileApi) -> Unit,
    onUpdate: () -> Unit,
) {
    val (pageState, _, snackMessage, updateKey) = viewModel.state.collectAsState().value
    // null if no file being downloaded
    var fileBeingDownloaded by remember { mutableStateOf<FileApi?>(null) }

    LaunchedEffect(Objects.hash(refreshKey)) {
        viewModel.performSearch()
    }

    // every time our controller makes an update to data, we trigger the app to refresh its views
    LaunchedEffect(updateKey) {
        if (updateKey > 0) {
            onUpdate()
        }
    }

    val directoryPicker = rememberFileSaverLauncher { selectedFile ->
        val f = fileBeingDownloaded
        if (selectedFile == null || f == null) {
            return@rememberFileSaverLauncher
        }
        viewModel.downloadFile(f, selectedFile)
        fileBeingDownloaded = null
    }

    /** used to trigger behavior when a context menu item is clicked */
    val onFileContextAction: (FileContextAction) -> Unit = {
        when (it) {
            is DownloadFileAction -> {
                directoryPicker.launch(it.file.nameWithoutExtension, it.file.extension)
            }

            is InfoFileAction -> {
                onFileClick(it.file)
            }

            is RenameFileAction -> viewModel.openRenameFileModal(it.file)
            is DeleteFileAction -> viewModel.openDeleteFileModal(it.file)
        }
    }

    Box {
        when (pageState) {
            is SearchResultsLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }

            is SearchResultsLoaded -> {
                val files = pageState.files
                if (files.isNotEmpty()) {
                    LazyVerticalGrid(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp,
                        ),
                        columns = GridCells.Adaptive(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(files) {
                            DesktopFileEntry(
                                file = it,
                                preview = pageState.previews[it.id],
                                onClick = onFileClick,
                                onDoubleClick = viewModel::openFile,
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
                                            transferable = DragAndDropTransferable(FolderChildSelection(it)),
                                            supportedActions = listOf(Move),
                                            dragDecorationOffset = offset,
                                            onTransferCompleted = {
                                                viewModel.performSearch()
                                            },
                                        )
                                    },
                                ),
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "No Files Found :(",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            is SearchResultsError -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "an error occurred",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "An error occurred during searching: ${pageState.message}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
        if (snackMessage != null) {
            // we have to use weird stuff like this because we're not using the Scaffold for the desktop app
            // TODO not important now, but make this animate in/out. Make root element be Box for this component.
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-16).dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                dismissAction = {
                    IconButton(onClick = { viewModel.clearMessage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss message")
                    }
                },
            ) {
                Text(text = snackMessage, modifier = Modifier.testTag("folderErrorMessage"))
            }
        }
    }
}

