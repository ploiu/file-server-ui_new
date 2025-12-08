package dev.ploiu.file_server_ui_new.components.sidesheet

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.*
import dev.ploiu.file_server_ui_new.components.dialog.Dialog
import dev.ploiu.file_server_ui_new.components.dialog.TextDialog
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import dev.ploiu.file_server_ui_new.util.getFileSizeAlias
import dev.ploiu.file_server_ui_new.util.toShortHandBytes
import dev.ploiu.file_server_ui_new.viewModel.*
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.runBlocking
import java.util.*


@Composable
// intellij believes that changing dialogState in lambda functions is useless, but they're very important
@Suppress("AssignedValueIsNeverRead")
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
    val filePicker = rememberFileSaverLauncher { file ->
        if (file != null) {
            viewModel.downloadFile(file)
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
                filePath = pageState.filePath,
                onRenameClick = { dialogState = RenameDialogState(pageState.file) },
                onSaveClick = {
                    println("save clicked!")
                    dialogState = NoDialogState()
                    filePicker.launch(pageState.file.nameWithoutExtension, pageState.file.extension)
                },
                onDeleteClick = { dialogState = DeleteDialogState(pageState.file) },
                onUpdateTags = { viewModel.updateTags(it) },
                onOpenClick = { viewModel.openFile() },
                preview = if (pageState is FilePreviewLoaded) {
                    pageState.preview
                } else {
                    null
                },
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
            Dialog(
                title = "Delete file",
                text = "Are you sure you want to delete?",
                confirmText = "Delete",
                dismissText = "Cancel",
                onDismissRequest = { dialogState = NoDialogState() },
                onConfirm = {
                    viewModel.deleteFile()
                    onChange()
                },
                icon = Icons.Default.Warning,
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
    onOpenClick: () -> Unit,
    onUpdateTags: (Collection<TaggedItemApi>) -> Unit,
    filePath: String,
    preview: ByteArray? = null,
) {
    val actionButtonColors: IconButtonColors = filledIconButtonColors()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("loadedRoot"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { // image and title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PickFileImage(file, preview, Modifier.width(108.dp).height(108.dp).testTag("fileImage"))
            Text(file.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("fileName"))
        }
        Spacer(Modifier.height(16.dp)) // file attributes
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            FlowRow(
                modifier = Modifier.padding(8.dp).testTag("fileMetadata"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val pillColors = pillColors(
                    backgroundColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = .12f)
                        .compositeOver(MaterialTheme.colorScheme.secondary),
                    contentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = .87f),
                )
                Pill(
                    filePath,
                    colors = pillColors,
                    textStyle = MaterialTheme.typography.labelLarge,
                )
                Pill(
                    text = "@type: ${file.fileType}",
                    colors = pillColors,
                    textStyle = MaterialTheme.typography.labelLarge,
                )
                Pill(
                    text = file.size.toShortHandBytes() + " (" + file.size.getFileSizeAlias() + ")",
                    colors = pillColors,
                    textStyle = MaterialTheme.typography.labelLarge,
                    icon = { Icon(Icons.Default.Straighten, contentDescription = "size icon") },
                )
                Pill(
                    text = file.dateCreated.split("T")[0],
                    textStyle = MaterialTheme.typography.labelLarge,
                    colors = pillColors,
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Date Created") },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TagList(file.tags, onUpdate = onUpdateTags)
        Spacer(Modifier.height(8.dp)) // action buttons TODO can probably pull out into common code
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDeleteClick, colors = actionButtonColors) {
                    Icon(Icons.Default.Delete, "Delete")
                }
                VerticalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.height(30.dp).padding(start = 4.dp, end = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                IconButton(onClick = onRenameClick, colors = actionButtonColors) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = onSaveClick, colors = actionButtonColors) {
                    Icon(Icons.Default.Save, "Save")
                }
                IconButton(onClick = onOpenClick, colors = actionButtonColors) {
                    Icon(Icons.Default.FileOpen, "Open")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
