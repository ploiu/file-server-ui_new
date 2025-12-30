package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
import dev.ploiu.file_server_ui_new.components.dialog.TextModalProps
import dev.ploiu.file_server_ui_new.model.*
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.FolderUploadService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
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
    private val fileService: FileService,
    private val folderUploadService: FolderUploadService,
    modalController: ModalController,
) : ViewModelWithModal(modalController) {
    private val log = KotlinLogging.logger("ApplicationViewModel")
    private val _state = MutableStateFlow(ApplicationUiModel(NoSideSheet()))
    val state = _state.asStateFlow()

    // TODO exception handler (look at folder detail view model)
    fun addEmptyFolder(name: String) = viewModelScope.launch(Dispatchers.IO) {
        folderService
            .createFolder(CreateFolder(name, 0L, listOf()))
            .onSuccess { /* TODO cause re-render */ }
            .onFailure { TODO("on Failure not handled for add empty folder") }
    }

    /**
     * can smartly upload multiple files and folders, reporting progress on every upload
     */
    fun uploadBulk(bulk: Collection<PlatformFile>, currentFolderId: Long) = viewModelScope.launch(Dispatchers.IO) {
        log.info { "bulk upload started!" }
        val folders = bulk.filter { it.isDirectory() }
        val files = bulk.filter { it.isRegularFile() }
        // make sure the files and folders don't have the exact same name, as that's not allowed on linux file systems
        val names = validateNameCollision(bulk) ?: return@launch
        folderService.hasNameClash(currentFolderId, names).map { hasClash ->
            if (hasClash) {
                Err("The folder already has folders or files with names matching what you selected. Check your selection and try again")
            } else {
                val errors = proceedUploadBulk(folders, files, currentFolderId)
                changeUpdateKey()
                if (errors.isNotEmpty()) {
                    closeModal()
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
                }
                log.info { "upload ended!" }
            }
        }.onFailure { msg ->
            closeModal()
            ErrorModal.open(
                ErrorModalProps(
                    title = "Failed to upload files",
                    text = msg,
                    icon = Icons.Default.Error,
                    iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                    onClose = this@ApplicationViewModel::closeModal,
                ),
            )
        }
    }

    /**
     * checks if any of the files passed have the same names with each other, and then returns those names (converted to lowercase)
     * if there is no collision
     *
     * @return `null` if any of the names collide, else returns a collection of the names in lowercase
     */
    private fun validateNameCollision(bulk: Collection<PlatformFile>): Collection<String>? {
        val names = mutableMapOf<String, Int>()
        for (file in bulk) {
            val name = file.name.lowercase()
            names[name] = if (name in names) {
                names[name]!! + 1
            } else {
                1
            }
        }
        if (names.any { it.value > 1 }) {
            ErrorModal.open(
                ErrorModalProps(
                    title = "Failed to upload files",
                    text = "Could not upload files, because multiple files with the same name were selected. Please check your selection and try again",
                    icon = Icons.Default.Error,
                    iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                    onClose = this@ApplicationViewModel::closeModal,
                ),
            )
            return null
        }
        return names.keys
    }

    /**
     * Handles actually uploading the files and folders passed to it for the passed folder id
     */
    private suspend fun proceedUploadBulk(
        folders: List<PlatformFile>,
        files: List<PlatformFile>,
        currentFolderId: Long,
    ): Collection<String> {
        // there's no name clash, we do a little uploading
        val approximatedFolders = folders.map { FolderApproximator.convertDir(it.file, 1) }
        val total = max(1, files.size + approximatedFolders.sumOf { it.size })
        val errors = mutableListOf<String>()
        LoadingModal.open(max = total, progress = 0)
        for (folder in folders) {
            // make sure we only upload 1 folder at a time. We batch its contents, but different calls to uploadFolder
            // (or uploadFile) can't communicate with each other. Calling these in parallel can easily overwhelm the server
            folderUploadService.uploadFolder(folder, currentFolderId).collect { res ->
                log.debug { "uploadFolder Got result $res" }
                updateModal<LoadingModal> { it.copy(progress = it.progress + 1) }
                if (res.errorMessage != null) {
                    errors += res.errorMessage
                }
            }
        }
        for (file in files) {
            fileService.createFile(CreateFileRequest(currentFolderId, file.file, false)).onFailure { errors += it }
            // regardless of if it fails, we need to bump the progress
            updateModal<LoadingModal> { it.copy(progress = it.progress + 1) }
        }
        closeModal()
        return errors
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
