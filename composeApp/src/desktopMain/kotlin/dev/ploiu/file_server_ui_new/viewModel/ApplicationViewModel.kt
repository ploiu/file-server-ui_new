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
import dev.ploiu.file_server_ui_new.model.FolderApproximator
import dev.ploiu.file_server_ui_new.service.BatchFolderUploadResult
import dev.ploiu.file_server_ui_new.service.BatchUploadFileResult
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.FolderUploadService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
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
class ApplicationViewModel(
    private val folderService: FolderService,
    private val folderUploadService: FolderUploadService,
    modalController: ModalController,
) :
    ViewModelWithModal(modalController) {
    private val log = KotlinLogging.logger("ApplicationViewModel")
    private val _state = MutableStateFlow(ApplicationUiModel(NoSideSheet()))
    val state = _state.asStateFlow()

    // TODO exception handler (look at folder detail view model)
    fun addEmptyFolder(name: String) = viewModelScope.launch(Dispatchers.IO) {
        folderService.createFolder(CreateFolder(name, 0L, listOf()))
            .onSuccess { /* TODO cause re-render */ }
            .onFailure { TODO("on Failure not handled for add empty folder") }
    }

    fun uploadFolder(folder: PlatformFile, currentFolderId: Long) = viewModelScope.launch(Dispatchers.IO) {
        closeModal()
        log.info { "upload started!" }
        var total: Int
        val errors = mutableListOf<String>()
        // TODO I don't like having to generate a second folder approximation since one is already being generated...maybe send a tuple, with the first element being the number of items?
        val approximation = FolderApproximator.convertDir(folder.file, 1)
        total = approximation.size
        // avoid division by zero
        if (total == 0) {
            total = 1
        }
        LoadingModal.open(max = total, progress = 0)
        folderUploadService.uploadFolder(folder, currentFolderId)
            .collect { result ->
                log.info { "Got result! $result" }
                when (result) {
                    is BatchUploadFileResult -> {
                        updateModal<LoadingModal> { it.copy(progress = it.progress + 1) }
                        if (result.errorMessage != null) {
                            errors.add(result.errorMessage)
                        }
                    }

                    is BatchFolderUploadResult -> {
                        // Could handle folder-level errors here
                        if (result.errorMessage != null) {
                            errors.add(result.errorMessage)
                        }
                    }
                }
            }
        modalController.close(this@ApplicationViewModel)
        if (errors.isNotEmpty()) {
            log.error {
                "Failed to upload part of or all of a folder:\n${errors.joinToString("\n")}"
            }
            ErrorModal.open(
                ErrorModalProps(
                    title = "Failed to upload folder",
                    text = "An error occurred attempting to upload the folder. Check server logs for details",
                    icon = Icons.Default.Error,
                    iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                    onClose = this@ApplicationViewModel::closeModal,
                ),
            )
            changeUpdateKey()
        } else {
            changeUpdateKey()
            log.info { "upload ended!" }
        }
    }

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

    fun openCreateEmptyFolderModal() {
        TextModal.open(
            TextModalProps(
                title = "Create empty folder",
                text = "Folder name",
                confirmText = "Create",
                onCancel = this::closeModal,
                onConfirm = {
                    if (it.isNotBlank()) {
                        closeModal()
                        addEmptyFolder(it)
                        changeUpdateKey()
                    }
                },
            ),
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    fun changeUpdateKey() {
        _state.update { it.copy(updateKey = Uuid.random().toString()) }
    }
}
