package dev.ploiu.file_server_ui_new.components.kindalazygrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.FileEntry
import dev.ploiu.file_server_ui_new.components.FolderEntry
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi

@Preview
@Composable
private fun WithPermanentItems() {
    KindaLazyVerticalGrid(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
        ),
        minColumnWidth = 150.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        val baseFolder = FolderApi(
            id = 0L,
            parentId = 0L,
            path = "/whatever",
            name = "some folder",
            folders = listOf(),
            files = listOf(),
            tags = listOf(),
        )
        val folders = (1..5L).map { baseFolder.copy(id = it) }
        val baseFile = FileApi(
            id = 0L,
            folderId = 0L,
            name = "some file",
            tags = listOf(),
            size = 0L,
            dateCreated = "",
            fileType = "application",
        )
        val files = (1..100L).map { baseFile.copy(id = it) }
        permanentItems = @Composable {
            for (folder in folders) {
                FolderEntry(
                    folder = folder,
                    onClick = {},
                )
            }
        }

        lazyItems = {
            items(files) {
                FileEntry(
                    file = it,
                )
            }
        }
    }
}
