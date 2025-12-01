package dev.ploiu.file_server_ui_new.components.sidesheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.Pill
import dev.ploiu.file_server_ui_new.components.TagList
import dev.ploiu.file_server_ui_new.components.TextDialog
import dev.ploiu.file_server_ui_new.components.pillColors
import dev.ploiu.file_server_ui_new.extensions.uiCount
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import dev.ploiu.file_server_ui_new.viewModel.*
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.draft
import file_server_ui_new.composeapp.generated.resources.folder
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import java.util.*

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun FolderDetailSheet(
    viewModel: FolderDetailViewModel,
    /** used when outside changes (e.g. from the main folder page) makes changes, so we know to refresh ourselves */
    refreshKey: Int,
    /** used if an action within this sheet should cause the sheet to close (e.g. when the folder is deleted) */
    closeSelf: () -> Unit,
    onChange: () -> Unit
) {
    val (pageState) = viewModel.state.collectAsState().value
    var dialogState: DialogState by remember { mutableStateOf(NoDialogState()) }
    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            viewModel.downloadFolder(directory)
        }
    }

    LaunchedEffect(Objects.hash(viewModel.folderId, refreshKey)) {
        viewModel.loadFolder()
    }

    when (pageState) {
        is FolderDetailLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(Modifier.testTag("spinner"))
            }
        }

        is FolderDetailHasFolder -> {
            MainFolderDetails(
                folder = pageState.folder,
                onRenameClick = { dialogState = RenameDialogState(pageState.folder) },
                onSaveClick = {
                    println("save clicked!")
                    dialogState = NoDialogState()
                    directoryPicker.launch()
                },
                onDeleteClick = { dialogState = DeleteDialogState(pageState.folder) },
                onUpdateTags = { viewModel.updateTags(it) })
            if (pageState is FolderDetailMessage) {
                Snackbar {
                    Text(pageState.message, modifier = Modifier.testTag("message"))
                }
            }
        }

        is FolderDeleted -> closeSelf()

        is FolderDetailErrored -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Error, "Error Icon", tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(32.dp))
                Text(pageState.message, textAlign = TextAlign.Center)
            }
        }
    }

    when (val state = dialogState) {
        is NoDialogState -> Unit /* No Op */

        is RenameDialogState -> {
            TextDialog(
                title = "Rename folder",
                defaultValue = (state.item as FolderApi).name,
                modifier = Modifier.testTag("renameDialog"),
                onCancel = { dialogState = NoDialogState() },
                onConfirm = {
                    dialogState = NoDialogState()
                    runBlocking {
                        viewModel.renameFolder(it)
                        onChange()
                    }
                }
            )
        }

        is DeleteDialogState -> {
            TextDialog(
                title = "Delete folder",
                bodyText = "Are you sure you want to delete? Type the folder name to confirm",
                modifier = Modifier.testTag("deleteDialog"),
                onCancel = { dialogState = NoDialogState() },
                onConfirm = {
                    dialogState = NoDialogState()
                    viewModel.deleteFolder(it)
                    onChange()
                }
            )
        }

    }
}

@Composable
private fun MainFolderDetails(
    folder: FolderApi,
    onRenameClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateTags: (Collection<TaggedItemApi>) -> Unit
) {
    val actionButtonColors: IconButtonColors = filledIconButtonColors()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("loadedRoot"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // image and title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.folder),
                contentDescription = "folder icon",
                Modifier.width(108.dp).height(108.dp).testTag("folderImage"),
                contentScale = ContentScale.Fit
            )
            Text(folder.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("folderName"))
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
                    icon = { Icon(Icons.Default.Folder, "Folder") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Pill(
                    text = folder.files.uiCount(),
                    colors = pillColors,
                    icon = { Icon(painterResource(Res.drawable.draft), "Files") }
                )
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
        // action buttons TODO can probably pull out into common code
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
