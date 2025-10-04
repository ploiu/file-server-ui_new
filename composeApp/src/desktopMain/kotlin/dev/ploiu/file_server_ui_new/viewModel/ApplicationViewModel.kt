package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.FolderUploadService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * represents modal state for the root application view, for things that aren't children of the main folder view or the side sheet
 */
sealed interface ApplicationModalState
object NoModal : ApplicationModalState
object CreatingEmptyFolder : ApplicationModalState
object SelectingFolderUpload : ApplicationModalState
data class ApplicationErrorModal(val message: String) : ApplicationModalState

/**
 * represents the state of the side sheet
 */
sealed interface SideSheetUiState
class NoSideSheet : SideSheetUiState
data class FileSideSheet(val file: FileApi) : SideSheetUiState
data class FolderSideSheet(val folder: FolderApi) : SideSheetUiState

data class ApplicationUiModel(
    val modalState: ApplicationModalState,
    val sideSheetState: SideSheetUiState,
    /** used to force refreshes of other components when the header causes something (e.g. a folder's contents via file/folder upload) to change */
    val headerUpdateKey: Int = 0
)

// I actually still don't like this but it's cleaner to handle it this way than a bunch of random handlers all over the place.
// However, this is only intended for the "root" application state. Things like the folder page, folder side sheet, etc should
// handle api calls in their own view models. This isn't a "catch all", this serves a small scope of the application
// (that just so happens to be top level)
class ApplicationViewModel(
    private val folderService: FolderService,
    private val fileService: FileService,
    private val folderUploadService: FolderUploadService
) :
    ViewModel() {
    private val log = KotlinLogging.logger("ApplicationViewModel")
    private val _state = MutableStateFlow(ApplicationUiModel(NoModal, NoSideSheet()))
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
        folderUploadService.uploadFolder(folder, currentFolderId)
            .onFailure { errors ->
                log.error {
                    "Failed to upload part of or all of a folder:\n${errors.joinToString("\n")}"
                }
                _state.update {
                    it.copy(
                        modalState = ApplicationErrorModal(message = "${errors.size} errors!"),
                        headerUpdateKey = it.headerUpdateKey + 1
                    )
                }
            }
            .onSuccess {
                _state.update { it.copy(headerUpdateKey = it.headerUpdateKey + 1) }
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

    fun openModal(which: ApplicationModalState) {
        _state.update { it.copy(modalState = which) }
    }

    fun closeModal() {
        _state.update { it.copy(modalState = NoModal) }
    }
}
