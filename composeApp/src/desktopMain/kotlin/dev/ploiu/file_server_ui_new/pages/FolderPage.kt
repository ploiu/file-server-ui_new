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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.Dialog
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.model.BatchFolderPreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.viewModel.FolderError
import dev.ploiu.file_server_ui_new.viewModel.FolderLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel


@Composable
fun FolderPage(view: FolderPageViewModel, onFolderNav: (FolderApi) -> Unit) {
    val (pageState, previews) = view.state.collectAsState().value

    LaunchedEffect(Unit) {
        view.loadFolder()
    }

    when (pageState) {
        is FolderLoading -> Column {
            CircularProgressIndicator()
        }

        is FolderLoaded -> LoadedFolderList(pageState.folder, previews, onFolderNav)
        is FolderError -> Dialog(
            title = "An Error Occurred",
            text = pageState.message,
            icon = Icons.Default.Error,
            iconColor = MaterialTheme.colorScheme.error
        )
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
private fun DesktopFolderEntry(folder: FolderApi, onClick: (f: FolderApi) -> Unit) {
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
            FolderEntry(folder, onClick = onClick)
        }
    }
}

@Composable
private fun LoadedFolderList(folder: FolderApi, previews: BatchFolderPreview, onFolderNav: (FolderApi) -> Unit) {
    val children: List<Any> =
        folder.folders.sortedBy { it.name } + folder.files.sortedByDescending { it.dateCreated }
    LazyVerticalGrid(
        // if this is changed, be sure to update SearchResultsPage.kt
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
