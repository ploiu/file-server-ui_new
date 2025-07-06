package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Snackbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.extensions.uiCount
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.Tag
import dev.ploiu.file_server_ui_new.viewModel.*
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.draft
import file_server_ui_new.composeapp.generated.resources.folder
import org.jetbrains.compose.resources.painterResource

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun FolderDetailSheet(viewModel: FolderDetailViewModel) {
    val (pageState) = viewModel.state.collectAsState().value

    LaunchedEffect(viewModel.folderId) {
        viewModel.loadFolder()
    }

    when (pageState) {
        is FolderDetailLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        is FolderDetailLoaded -> {
            MainFolderDetails(
                folder = pageState.folder,
                onRenameClick = { TODO("Rename functionality") },
                onSaveClick = { TODO("Save functionality") },
                onDeleteClick = { TODO("Delete functionality") },
                onUpdateTags = { viewModel.updateTags(it) })
        }

        is FolderDetailErrored -> TODO()

        is FolderDetailNonCriticalError -> {
            MainFolderDetails(
                folder = pageState.folder,
                onRenameClick = { TODO("Rename functionality") },
                onSaveClick = { TODO("Save functionality") },
                onDeleteClick = { TODO("Delete functionality") },
                onUpdateTags = { viewModel.updateTags(it) })
            Snackbar {
                Text(pageState.message)
            }
        }
    }
}

@Composable
private fun MainFolderDetails(
    folder: FolderApi,
    onRenameClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateTags: (Collection<Tag>) -> Unit
) {
    val actionButtonColors: IconButtonColors = filledIconButtonColors()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // image and title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.folder),
                contentDescription = "folder icon",
                Modifier.width(108.dp).height(108.dp),
                contentScale = ContentScale.Fit
            )
            Text(folder.name, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val pillColors = pillColors(
                    backgroundColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = .12f)
                        .compositeOver(MaterialTheme.colorScheme.secondary),
                    contentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = .87f),
                )
                Pill(
                    text = folder.folders.uiCount(),
                    colors = pillColors,
                    icon = { Icon(Icons.Default.Folder, "Folder") })
                Spacer(modifier = Modifier.width(8.dp))
                Pill(
                    text = folder.files.uiCount(),
                    colors = pillColors,
                    icon = { Icon(painterResource(Res.drawable.draft), "Files") })
                Spacer(modifier = Modifier.width(8.dp))
                Pill(
                    "~/" + folder.path,
                    colors = pillColors,
                    textStyle = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TagList(folder.tags, onUpdate = onUpdateTags)
        Spacer(Modifier.height(8.dp))
        // action buttons
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = onRenameClick, colors = actionButtonColors) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = onSaveClick, colors = actionButtonColors) {
                    Icon(Icons.Default.Save, "Save")
                }
                IconButton(onClick = onDeleteClick, colors = actionButtonColors) {
                    Icon(Icons.Default.Delete, "Delete")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
