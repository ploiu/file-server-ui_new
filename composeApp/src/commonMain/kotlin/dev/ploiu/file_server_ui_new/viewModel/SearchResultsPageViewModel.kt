package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.*
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
import dev.ploiu.file_server_ui_new.components.dialog.PromptModalProps
import dev.ploiu.file_server_ui_new.components.dialog.TextModalProps
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.PreviewService
import dev.ploiu.file_server_ui_new.util.getCachedFile
import dev.ploiu.file_server_ui_new.util.writeTempFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.Serializable
import org.koin.ext.getFullName
import kotlin.use

@Serializable
class SearchResultsRoute(val searchTerm: String)

sealed interface SearchResultsUiState
class SearchResultsLoading : SearchResultsUiState
data class SearchResultsLoaded(
    val files: List<FileApi>,
    val previews: BatchFilePreview,
) : SearchResultsUiState

class SearchResultsError(val message: String) : SearchResultsUiState


data class SearchResultsPageUiModel(
    val pageState: SearchResultsUiState,
    val searchTerm: String,
    /** For non-critical messages that show as a snackbar */
    val message: String? = null,
    /** used to signal to the main app view that it needs to refresh its view */
    val updateKey: Int = 0,
)

class SearchResultsPageViewModel(
    val fileService: FileService,
    val previewService: PreviewService,
    val searchTerm: String,
    modalController: ModalController,
) : ViewModelWithModal(modalController) {
    private val _state = MutableStateFlow(SearchResultsPageUiModel(SearchResultsLoading(), searchTerm))
    val state = _state.asStateFlow()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _state.update {
            val errorMessage = throwable.message ?: "Unknown error: ${throwable.javaClass.name}"
            it.copy(pageState = SearchResultsError(message = errorMessage))
        }
    }

    fun performSearch() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val res = fileService.search(searchTerm)
        res.onSuccess { files ->
            val sorted = files
                .toList()
                .sortedWith(
                    compareBy<FileApi> { it.name.lowercase() }.thenBy { it.dateCreated }.thenBy { it.id },
                )
            _state.update { it.copy(pageState = SearchResultsLoaded(sorted, mapOf())) }
            val previews = previewService.getFilePreviews(*files.toTypedArray())
            if (previews.isOk) {
                _state.update {
                    if (it.pageState is SearchResultsLoaded) {
                        it.copy(pageState = it.pageState.copy(previews = previews.unwrap()))
                    } else it
                }
            } else {
                TODO("preview error state")
            }
        }.onFailure { message ->
            _state.update { it.copy(pageState = SearchResultsError(message)) }
        }
    }

    fun openFile(file: FileApi) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        // don't re-fetch the file if it's already cached
        val cached = getCachedFile(file.id, file.name)
        if (cached != null) {
            FileKit.openFileWithDefaultApplication(cached)
            return@launch
        }
        fileService.getFileContents(file.id).flatMap { res ->
            res.use { inputStream -> writeTempFile(file.id, file.name, inputStream) }.mapEither(
                success = { it },
                failure = { e -> e.message ?: (e::class.getFullName() + " (no exception message)") },
            )
        }.onSuccess(FileKit::openFileWithDefaultApplication).onFailure { msg ->
            ErrorModal.open(
                ErrorModalProps(
                    title = "Failed to open file",
                    text = msg,
                    icon = Icons.Default.Error,
                    iconColorProvider = @Composable { MaterialTheme.colorScheme.error },
                    onClose = this@SearchResultsPageViewModel::closeModal,
                ),
            )
        }

    }

    /**
     * downloads a file and saves it to the user-requested location
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

    fun updateFile(file: FileApi) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.value.pageState
        if (currentState is SearchResultsLoaded) {
            performSearch()
            val updateRes = fileService.updateFile(file.toFileRequest())
            updateRes.onSuccess {
                // we don't re-pull the folder because we update the update key, which forces everything in the app to sync
                _state.update { it.copy(updateKey = it.updateKey + 1) }
            }.onFailure { openErrorModal(message = it) }
        }
    }

    /**
     * deletes the passed [file] and updates our updateKey if successful
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

    /** clears out errorMessage */
    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
