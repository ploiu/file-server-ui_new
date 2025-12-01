package dev.ploiu.file_server_ui_new.components.sidesheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
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
import dev.ploiu.file_server_ui_new.components.*
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import dev.ploiu.file_server_ui_new.viewModel.*
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import java.util.*


/*
    TODO
      1) get file path
      2) get file preview
      3) get file stats for display with file path
      4) regular confirm dialog for delete
      5)
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun FileDetailSheet(
    viewModel: FileDetailViewModel,
    /** used when outside changes (e.g. from the main file page) makes changes, so we know to refresh ourselves */
    refreshKey: Int,
    /** used if an action within this sheet should cause the sheet to close (e.g. when the file is deleted) */
    closeSelf: () -> Unit,
    onChange: () -> Unit,
) {
    val (pageState) = viewModel.state.collectAsState().value
    var dialogState: DialogState by remember { mutableStateOf(NoDialogState()) }
    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            viewModel.downloadFile(directory)
        }
    }

    LaunchedEffect(Objects.hash(viewModel.fileId, refreshKey)) {
        viewModel.loadFile()
    }

    when (pageState) {
        is FileDetailLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(Modifier.testTag("spinner"))
            }
        }

        is FileDetailHasFile -> {
            MainFileDetails(
                file = pageState.file,
                onRenameClick = { dialogState = RenameDialogState(pageState.file) },
                onSaveClick = {
                    println("save clicked!")
                    dialogState = NoDialogState()
                    directoryPicker.launch()
                },
                onDeleteClick = { dialogState = DeleteDialogState(pageState.file) },
                onUpdateTags = { viewModel.updateTags(it) },
            )
            if (pageState is FileDetailMessage) {
                Snackbar {
                    Text(pageState.message, modifier = Modifier.testTag("message"))
                }
            }
        }

        is FileDeleted -> closeSelf()

        is FileDetailErrored -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
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
                title = "Rename file",
                defaultValue = (state.item as FileApi).name,
                modifier = Modifier.testTag("renameDialog"),
                onCancel = { dialogState = NoDialogState() },
                onConfirm = {
                    dialogState = NoDialogState()
                    runBlocking {
                        viewModel.renameFile(it)
                        onChange()
                    }
                },
            )
        }

        is DeleteDialogState -> {
            TODO("regular confirm dialog")
            TextDialog(
                title = "Delete file",
                bodyText = "Are you sure you want to delete? Type the file name to confirm",
                modifier = Modifier.testTag("deleteDialog"),
                onCancel = { dialogState = NoDialogState() },
                onConfirm = {
                    dialogState = NoDialogState()
                    viewModel.deleteFile(it)
                    onChange()
                },
            )
        }

    }
}

@Composable
private fun MainFileDetails(
    file: FileApi,
    onRenameClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateTags: (Collection<TaggedItemApi>) -> Unit,
) {
    val actionButtonColors: IconButtonColors = filledIconButtonColors()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("loadedRoot"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // image and title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(determineIcon(file)),
                contentDescription = "file icon",
                Modifier.width(108.dp).height(108.dp).testTag("fileImage"),
                contentScale = ContentScale.Fit,
            )
            Text(file.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("fileName"))
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val pillColors = pillColors(
                    backgroundColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = .12f)
                        .compositeOver(MaterialTheme.colorScheme.secondary),
                    contentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = .87f),
                )
                // TODO more file stats like date created and size
                Spacer(modifier = Modifier.width(8.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Pill(
                    // TODO get full file path
                    "~/" + file.name,
                    colors = pillColors,
                    textStyle = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TagList(file.tags, onUpdate = onUpdateTags)
        Spacer(Modifier.height(8.dp))
        // action buttons TODO can probably pull out into common code
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
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
