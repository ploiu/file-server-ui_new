package dev.ploiu.file_server_ui_new.components.sidesheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.components.PickFileImage
import dev.ploiu.file_server_ui_new.components.Pill
import dev.ploiu.file_server_ui_new.components.TagList
import dev.ploiu.file_server_ui_new.components.pillColors
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import dev.ploiu.file_server_ui_new.util.getFileSizeAlias
import dev.ploiu.file_server_ui_new.util.toShortHandBytes
import dev.ploiu.file_server_ui_new.viewModel.*
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import java.util.*


@Composable
// intellij believes that changing dialogState in lambda functions is useless, but they're very important
@OptIn(ExperimentalMaterialApi::class)
fun FileDetailSheet(
    viewModel: FileDetailViewModel,
    /** used when outside changes (e.g. from the main file page) makes changes, so we know to refresh ourselves */
    refreshKey: String,
    /** used if an action within this sheet should cause the sheet to close (e.g. when the file is deleted) */
    closeSelf: () -> Unit,
    onChange: () -> Unit,
) {
    val (pageState, updateKey) = viewModel.state.collectAsState().value
    val filePicker = rememberFileSaverLauncher { file ->
        if (file != null) {
            viewModel.downloadFile(file)
        }
    }

    // for when a new file is selected
    LaunchedEffect(Objects.hash(viewModel.fileId, refreshKey)) {
        viewModel.loadFile()
    }

    // for when a change is made and we need to signal to the parent to refresh
    LaunchedEffect(updateKey) {
        if (updateKey > 0) {
            onChange()
        }
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
                onRenameClick = viewModel::openRenameModal,
                onSaveClick = {
                    println("save clicked!")
                    viewModel.closeModal()
                    filePicker.launch(pageState.file.nameWithoutExtension, pageState.file.extension)
                },
                onDeleteClick = viewModel::openDeleteDialog,
                onUpdateTags = { viewModel.updateTags(it) },
                onOpenClick = { viewModel.openFile() },
                onAddTagClicked = viewModel::openAddTagDialog,
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
}

@Composable
private fun MainFileDetails(
    file: FileApi,
    onRenameClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenClick: () -> Unit,
    onUpdateTags: (Collection<TaggedItemApi>) -> Unit,
    onAddTagClicked: () -> Unit,
    filePath: String,
    preview: ByteArray? = null,
) {
    val actionButtonColors: IconButtonColors = filledIconButtonColors()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("loadedRoot"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // image and title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PickFileImage(file, preview, Modifier.width(108.dp).height(108.dp).testTag("fileImage"))
            Text(file.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("fileName"))
        }
        Spacer(Modifier.height(16.dp))
        // file attributes
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
                    backgroundColor = MaterialTheme.colorScheme.onSecondary
                        .copy(alpha = .12f)
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
        TagList(file.tags, onDelete = onUpdateTags, onAddClick = onAddTagClicked)
        Spacer(Modifier.height(8.dp))
        // action buttons TODO can probably pull out into common code
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
