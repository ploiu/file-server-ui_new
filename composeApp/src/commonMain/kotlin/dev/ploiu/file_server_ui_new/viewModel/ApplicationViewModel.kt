package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
import dev.ploiu.file_server_ui_new.components.dialog.TextModalProps
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderChild
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * represents the state of the side sheet
 */
sealed interface SideSheetUiState
class NoSideSheet : SideSheetUiState
data class FileSideSheet(val file: FileApi) : SideSheetUiState
data class FolderSideSheet(val folder: FolderApi) : SideSheetUiState

data class ApplicationUiModel @OptIn(ExperimentalUuidApi::class) constructor(
    val sideSheetState: SideSheetUiState,
    /** used to force refreshes of other components when the header causes something (e.g. a folder's contents via file/folder upload) to change */
    val updateKey: String = Uuid.random().toString(),
)

// I actually still don't like this but it's cleaner to handle it this way than a bunch of random handlers all over the place.
// However, this is only intended for the "root" application state. Things like the folder page, folder side sheet, etc. should
// handle api calls in their own view models. This isn't a "catch all", this serves a small scope of the application
// (that just so happens to be top level)
abstract class ApplicationViewModel(
    protected val folderService: FolderService,
    protected val fileService: FileService,
    modalController: ModalController,
) : ViewModelWithModal(modalController) {
    protected val _state = MutableStateFlow(ApplicationUiModel(NoSideSheet()))
    val state = _state.asStateFlow()

    fun addEmptyFolder(name: String, currentFolderId: Long) = viewModelScope.launch(Dispatchers.IO) {
        folderService
            .createFolder(CreateFolder(name, currentFolderId, listOf()))
            .onSuccess { changeUpdateKey() }
            .onFailure {
                ErrorModal.open(
                    ErrorModalProps(
                        title = "Failed to add folder",
                        text = "Failed to add folder: $it",
                        icon = Icons.Default.Error,
                        iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                        onClose = { this@ApplicationViewModel.closeModal() },
                    ),
                )
            }
    }

    /**
     * can smartly upload multiple files and folders, reporting progress on every upload
     */
    abstract fun uploadBulk(bulk: Collection<PlatformFile>, currentFolderId: Long): Job

    fun closeSideSheet() {
        _state.update { it.copy(sideSheetState = NoSideSheet()) }
    }

    fun sideSheetItem(item: Any?) {
        val sheetState = when (item) {
            null -> NoSideSheet()
            is FileApi -> FileSideSheet(item)
            is FolderApi -> FolderSideSheet(item)
            else -> throw UnsupportedOperationException("object of type ${item.javaClass} is not null, FileApi, or FolderApi")
        }
        _state.update { it.copy(sideSheetState = sheetState) }
    }

    fun openCreateEmptyFolderModal(currentFolderId: Long) {
        TextModal.open(
            TextModalProps(
                title = "Create empty folder",
                text = "Folder name",
                confirmText = "Create",
                onCancel = this::closeModal,
                onConfirm = {
                    if (it.isNotBlank()) {
                        closeModal()
                        addEmptyFolder(name = it, currentFolderId = currentFolderId)
                    }
                },
            ),
        )
    }

    /**
     * attempts to move a [FolderChild] to a new parent [FolderApi]
     */
    fun moveChildToFolder(newParent: FolderApi, child: FolderChild) = viewModelScope.launch(Dispatchers.IO) {
        // we can rely on the server to check that the new parent doesn't have this child already
        when (child) {
            is FileApi -> {
                val req = child.toFileRequest().copy(folderId = newParent.id)
                fileService.updateFile(req)
            }

            is FolderApi -> {
                val req = child.toUpdateFolder().copy(parentId = newParent.id)
                folderService.updateFolder(req)
            }
        }.onSuccess { changeUpdateKey() }.onFailure {
            ErrorModal.open(
                ErrorModalProps(
                    title = "Failed to move item",
                    text = "Failed to move item to parent folder: $it",
                    icon = Icons.Default.Error,
                    iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                    onClose = { this@ApplicationViewModel.closeModal() },
                ),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun changeUpdateKey() {
        _state.update { it.copy(updateKey = Uuid.random().toString()) }
    }
}
