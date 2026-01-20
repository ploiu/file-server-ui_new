package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.components.kindalazygrid.KindaLazyScrollState
import dev.ploiu.file_server_ui_new.components.kindalazygrid.KindaLazyVerticalGrid
import dev.ploiu.file_server_ui_new.components.kindalazygrid.rememberKindaLazyScrollState
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderChild
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderPageLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel

@Composable
fun FolderPage(
    viewModel: FolderPageViewModel,
    onFolderNav: (FolderApi) -> Unit,
) {
    val (pageState, previews, updateKey, errorMessage) = viewModel.state.collectAsState().value
    val scrollState = rememberKindaLazyScrollState()

    LaunchedEffect(viewModel.folderId) {
        viewModel.loadFolder()
    }

    when (pageState) {
        is FolderPageLoading -> CircularProgressIndicator()
        is FolderPageLoaded -> Box {
            LoadedFolderList(
                folder = pageState.folder,
                previews = previews,
                isDragging = false,
                scrollState = scrollState,
                onFolderNav = onFolderNav,
                onFolderChildDropped = { _, _ -> },
            )
        }
    }

}

@Composable
private fun LoadedFolderList(
    folder: FolderApi,
    previews: BatchFilePreview,
    /** is any drag and drop action being performed */
    isDragging: Boolean,
    scrollState: KindaLazyScrollState,
    /** callback when the user wants to navigate to a folder */
    onFolderNav: (FolderApi) -> Unit,
    /** when a folder or a file is dragged and dropped to another folder */
    onFolderChildDropped: (FolderApi, FolderChild) -> Unit,
) {
    val folders = folder.folders.sortedBy { it.name }
    val files = folder.files.sortedByDescending { it.dateCreated }

    val contentPadding = PaddingValues(
        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
    )

    KindaLazyVerticalGrid(
        contentPadding = contentPadding,
        minColumnWidth = 150.dp,
        modifier = Modifier.navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        scrollState = scrollState,
        permanentChildren = folders,
        lazyChildren = files,
    ) {
        permanentTemplate = { item ->
            FolderEntry(folder = item, modifier = Modifier.fillMaxWidth(), onClick = { onFolderNav(it) })
        }

        lazyTemplate = { item ->
            FileEntry(file = item)
        }
    }
}
