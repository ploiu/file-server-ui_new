package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.mapEither
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
import dev.ploiu.file_server_ui_new.components.dialog.PromptModalProps
import dev.ploiu.file_server_ui_new.components.dialog.TextModalProps
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.PreviewService
import dev.ploiu.file_server_ui_new.util.getCachedFile
import dev.ploiu.file_server_ui_new.util.writeTempFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.Serializable
import org.koin.ext.getFullName

@Serializable
data class FolderRoute(val id: Long)

sealed interface FolderPageUiState
class FolderPageLoading : FolderPageUiState
data class FolderPageLoaded(val folder: FolderApi) : FolderPageUiState

data class FolderPageUiModel(
    val pageState: FolderPageUiState,
    val previews: BatchFilePreview,
    /** used to signal to the main app view that it needs to refresh its view */
    val updateKey: Int = 0,
    /** for non-critical messages that show as a snackbar. Not part of a "HasFolder" state because it's just simpler to do it this way since it doesn't impact main page state */
    val message: String? = null,
)

class FolderPageViewModel(
    private val folderService: FolderService,
    private val fileService: FileService,
    private val previewService: PreviewService,
    val folderId: Long,
    modalController: ModalController,
) : ViewModelWithModal(modalController) {
    private val _state = MutableStateFlow(FolderPageUiModel(FolderPageLoading(), emptyMap()))
    val state = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _state.update {
            val errorMessage = throwable.message ?: "Unknown error: ${throwable.javaClass.name}"
            it.copy(message = errorMessage)
        }
    }

    fun loadFolder() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        _state.update { it.copy(pageState = FolderPageLoading()) }
        val folderRes = folderService.getFolder(folderId)
        folderRes.onSuccess { folder ->
            _state.update { it.copy(pageState = FolderPageLoaded(folder)) }
            // previews can now be pulled since we know what to pull. Errors are non-critical so they don't show a dialog
            previewService.getFolderPreview(folder).onEach { preview ->
                _state.update {
                    val previews = it.previews.toMutableMap()
                    previews[preview.first] = preview.second
                    it.copy(previews = previews)
                }
            }.catch { e ->
                log.error(e) { "Failed to load previews: $e" }
                _state.update { it.copy(message = "Failed to load previews: ${e.message}") }
            }.launchIn(this)
        }.onFailure { error ->
            // failed to pull folder at _all_
            openErrorModal(message = error)
            log.error { "Failed to get folder information: $error" }
        }
    }

    /**
     * Handles downloading the folder to the passed [downloadedFolder].
     */
    fun downloadFolder(folder: FolderApi, downloadedFolder: PlatformFile) =
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            folderService.downloadFolder(folder.id).onSuccess { res ->
                downloadedFolder.sink(false).buffered().use { sink ->
                    sink.transferFrom(res.asSource())
                }
                res.close()
                _state.update { it.copy(message = "Folder downloaded successfully") }
            }.onFailure { msg ->
                _state.update {
                    it.copy(message = msg)
                }
            }
        }

    /** clears out errorMessage */
    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun updateFolder(folder: FolderApi) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.value.pageState
        if (currentState is FolderPageLoaded) {
            _state.update { it.copy(pageState = FolderPageLoading()) }
            val updateRes = folderService.updateFolder(folder.toUpdateFolder())
            updateRes.onSuccess {
                // TODO going round trip to the server just to update 1 folder might be wasteful.
                //  Might be better to just update the folder in our existing model without
                //  having to pull again...will need refresh logic though (ctrl + r)
                // we don't re-pull the folder because we update the update key, which forces everything in the app to sync
                _state.update { it.copy(updateKey = it.updateKey + 1) }
            }.onFailure { openErrorModal(message = it) }
        }
    }

    /**
     * deletes the passed [folder] and calls [loadFolder] if successful.
     * If there's an error, the state is updated to have the error message
     */
    fun deleteFolder(folder: FolderApi) = viewModelScope.launch(Dispatchers.IO) {
        folderService.deleteFolder(folder.id).onSuccess {
            // we don't re-pull the folder because we update the update key, which forces everything in the app to sync
            _state.update { it.copy(message = "Folder deleted successfully", updateKey = it.updateKey + 1) }
        }.onFailure { msg ->
            _state.update { it.copy(message = msg) }
        }
    }

    /**
     * Handles downloading the file to the passed [saveFile].
     */
    fun downloadFile(file: FileApi, saveFile: PlatformFile) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        fileService.getFileContents(file.id).onSuccess { res ->
            saveFile.sink(false).buffered().use { sink ->
                sink.transferFrom(res.asSource())
            }
            res.close()
            _state.update { it.copy(message = "File downloaded successfully") }
        }.onFailure { openErrorModal(message = it) }
    }

    fun updateFile(file: FileApi) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.value.pageState
        if (currentState is FolderPageLoaded) {
            _state.update { it.copy(pageState = FolderPageLoading()) }
            val updateRes = fileService.updateFile(file.toFileRequest())
            updateRes.onSuccess {
                // we don't re-pull the folder because we update the update key, which forces everything in the app to sync
                _state.update { it.copy(updateKey = it.updateKey + 1) }
            }.onFailure { openErrorModal(message = it) }
        }
    }

    /**
     * deletes the passed [file] and calls [loadFolder] if successful.
     * If there's an error, the state is updated to have the error message
     */
    fun deleteFile(file: FileApi) = viewModelScope.launch(Dispatchers.IO) {
        fileService.deleteFile(file.id).onSuccess {
            // we don't re-pull the folder because we update the update key, which forces everything in the app to sync
            _state.update { it.copy(message = "File deleted successfully", updateKey = it.updateKey + 1) }
        }.onFailure { msg ->
            _state.update { it.copy(message = msg) }
        }
    }

    fun openDeleteFolderModal(folder: FolderApi) {
        TextModal.open(
            TextModalProps(
                title = "Delete folder",
                text = "Are you sure you want to delete this folder? Type the name to confirm",
                confirmText = "Delete",
                onCancel = this::closeModal,
                onConfirm = {
                    if (it == folder.name) {
                        closeModal()
                        deleteFolder(folder)
                    }
                },
            ),
        )
    }

    fun openRenameFolderModal(folder: FolderApi) {
        TextModal.open(
            TextModalProps(
                title = "Rename folder",
                defaultValue = folder.name,
                onCancel = this::closeModal,
                onConfirm = {
                    closeModal()
                    updateFolder(folder.copy(name = it))
                },
            ),
        )
    }

    fun openRenameFileModal(file: FileApi) {
        TextModal.open(
            TextModalProps(
                title = "Rename file",
                defaultValue = file.name,
                onCancel = this::closeModal,
                onConfirm = {
                    updateFile(file.copy(name = it))
                    closeModal()
                },
            ),
        )
    }

    fun openDeleteFileModal(file: FileApi) {
        ConfirmModal.open(
            PromptModalProps(
                title = "Delete file",
                text = "Are you sure you want to delete this file?",
                confirmText = "Delete",
                icon = Icons.Default.Warning,
                onCancel = this::closeModal,
                onConfirm = {
                    deleteFile(file)
                    closeModal()
                },
            ),
        )
    }

    fun openErrorModal(title: String = "An error occurred", message: String) {
        ErrorModal.open(
            ErrorModalProps(
                title = title,
                text = message,
                icon = Icons.Default.Error,
                iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                onClose = this::closeModal,
            ),
        )
    }

    fun openFile(file: FileApi) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val cached = getCachedFile(file.id, file.name)
        if (cached != null) {
            FileKit.openFileWithDefaultApplication(cached)
            return@launch
        }
        fileService.getFileContents(file.id).flatMap { res ->
            res.use { inputStream ->
                writeTempFile(file.id, file.name, inputStream)
            }.mapEither(
                success = FileKit::openFileWithDefaultApplication,
                failure = { e -> e.message ?: (e::class.getFullName() + " (no exception message)") },
            )
        }.onFailure { msg ->
            openErrorModal(
                title = "Failed to open file",
                message = msg,
            )
        }
    }
}
