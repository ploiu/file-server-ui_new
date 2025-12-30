package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.components.dialog.PromptModalProps
import dev.ploiu.file_server_ui_new.components.dialog.TextModalProps
import dev.ploiu.file_server_ui_new.model.*
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.PreviewService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.outputStream

/** controls the display of the entire file detail view sheet itself */
sealed interface FileDetailUiState

/** used for when a file is renderable on the ui */
sealed interface FileDetailHasFile {
    val file: FileApi
    val folder: FolderApi
    val filePath get() = file.path(folder)
}

class FileDetailLoading : FileDetailUiState
data class FileDetailLoaded(override val file: FileApi, override val folder: FolderApi) : FileDetailUiState,
    FileDetailHasFile

data class FilePreviewLoaded(override val file: FileApi, override val folder: FolderApi, val preview: ByteArray) :
    FileDetailUiState, FileDetailHasFile {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilePreviewLoaded

        if (file != other.file) return false
        if (folder != other.folder) return false
        if (!preview.contentEquals(other.preview)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + folder.hashCode()
        result = 31 * result + preview.contentHashCode()
        return result
    }
}

class FileDeleted : FileDetailUiState

data class FileDetailErrored(val message: String) : FileDetailUiState

/** used if a non-critical error occurs (such as failing to delete a tag, or failing to load a preview) */
data class FileDetailMessage(override val file: FileApi, override val folder: FolderApi, val message: String) :
    FileDetailUiState, FileDetailHasFile

data class FileDetailUiModel(
    val sheetState: FileDetailUiState,
    val updateKey: Int = 0,
)

// TODO this should be pulled into common code, not desktop module (look at why MutableStateFlow isn't accessible in commonMain)
class FileDetailViewModel(
    private val fileService: FileService,
    private val folderService: FolderService,
    private val previewService: PreviewService,
    val fileId: Long,
    modalController: ModalController,
) : ViewModelWithModal(modalController) {
    private val log = KotlinLogging.logger { }
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        log.error(throwable) {
            "Failed to process file information"
        }
        _state.update {
            it.copy(
                sheetState = FileDetailErrored(
                    "Failed to process file information: " + (throwable.message ?: throwable.javaClass),
                ),
            )
        }
    }
    private val _state = MutableStateFlow(FileDetailUiModel(FileDetailLoading()))
    val state = _state.asStateFlow()

    fun loadFile() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        _state.update { it.copy(sheetState = FileDetailLoading()) }
        val fileRes = fileService.getMetadata(fileId)
        fileRes
            .andThen { f -> folderService.getFolder(f.folderId).map { folder -> folder to f } }
            .onSuccess { pairing ->
                val file = pairing.second
                val folder = pairing.first
                _state.update { it.copy(sheetState = FileDetailLoaded(file = file, folder = folder)) }
                getPreview(file)
            }
            .onFailure { msg -> _state.update { it.copy(sheetState = FileDetailErrored("Failed to load file information: $msg")) } }
    }

    private fun getPreview(file: FileApi) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            previewService.getFilePreviews(file).onSuccess { previews ->
                val preview = previews[currentState.file.id]
                if (preview != null) {
                    _state.update {
                        it.copy(
                            sheetState = FilePreviewLoaded(
                                file = currentState.file,
                                folder = currentState.folder,
                                preview = preview,
                            ),
                        )
                    }
                }
            }.onFailure { msg ->
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            file = currentState.file,
                            folder = currentState.folder,
                            message = "Failed to load preview for file: $msg",
                        ),
                    )
                }
            }
        }
    }

    fun renameFile(newName: String) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailLoaded) {
            val toUpdate = currentState.file.copy(name = newName).toFileRequest()
            updateFile(toUpdate)
        }
    }

    fun deleteFile() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            fileService.deleteFile(currentState.file.id).onSuccess {
                _state.update { it.copy(sheetState = FileDeleted()) }
            }.onFailure { msg ->
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            file = currentState.file,
                            folder = currentState.folder,
                            message = msg,
                        ),
                    )
                }
            }
        }
    }

    fun openFile() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            // TODO move this to cross-platform version
            val tempFile = createTempFile()
            fileService.getFileContents(currentState.file.id).onSuccess { res ->
                res.use { inputStream ->
                    tempFile.outputStream().use { os ->
                        inputStream.transferTo(os)
                    }
                }
                val file = PlatformFile(file = tempFile.toFile())
                FileKit.openFileWithDefaultApplication(file)
            }.onFailure { msg ->
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            file = currentState.file,
                            folder = currentState.folder,
                            message = msg,
                        ),
                    )
                }
                tempFile.deleteExisting()
            }
        }

    }

    fun updateTags(tags: Collection<TaggedItemApi>) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            val toUpdate = currentState.file.copy(tags = tags).toFileRequest()
            updateFile(toUpdate)
        }
    }

    fun clearNonCriticalError() {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailMessage) {
            _state.update {
                it.copy(
                    sheetState = FileDetailLoaded(
                        file = currentState.file,
                        folder = currentState.folder,
                    ),
                )
            }
        }
    }

    fun downloadFile(saveFile: PlatformFile) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            fileService.getFileContents(currentState.file.id).onSuccess { res ->
                saveFile.sink(false).buffered().use { sink ->
                    sink.transferFrom(res.asSource())
                    _state.update {
                        it.copy(
                            sheetState = FileDetailMessage(
                                folder = currentState.folder,
                                file = currentState.file,
                                message = "Successfully saved file",
                            ),
                        )
                    }
                }
                res.close()
            }.onFailure { msg ->
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            file = currentState.file,
                            message = msg,
                            folder = currentState.folder,
                        ),
                    )
                }
            }
        }
    }

    fun openRenameModal() {
        val current = _state.value.sheetState
        if (current is FileDetailHasFile) {
            val file = current.file
            openModal(
                TextModal(
                    TextModalProps(
                        title = "Rename file",
                        modifier = Modifier.testTag("renameDialog"),
                        confirmText = "Rename",
                        defaultValue = file.name,
                        onCancel = this::closeModal,
                        onConfirm = {
                            closeModal()
                            renameFile(it)
                            _state.update { old -> old.copy(updateKey = old.updateKey + 1) }
                        },
                    ),
                ),
            )
        }
    }

    fun openDeleteDialog() {
        ConfirmModal.open(
            PromptModalProps(
                title = "Delete file",
                text = "Are you sure you want to delete?",
                confirmText = "Delete",
                onCancel = this::closeModal,
                onConfirm = {
                    deleteFile()
                    _state.update { it.copy(updateKey = it.updateKey + 1) }
                    closeModal()
                },
            ),
        )
    }

    fun openAddTagDialog() {
        TextModal.open(
            TextModalProps(
                title = "Add tag",
                confirmText = "Add",
                onCancel = this::closeModal,
                onConfirm = {
                    val current = _state.value.sheetState
                    if (current is FileDetailHasFile) {
                        val fileTags = current.file.tags
                        val newTags = fileTags.toMutableSet()
                        newTags.add(TaggedItemApi(id = null, title = it.lowercase(), implicitFrom = null))
                        closeModal()
                        updateTags(newTags)
                    }
                },
            ),
        )
    }

    private fun updateFile(toUpdate: FileRequest) =
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) { // this is nice just in case
            clearNonCriticalError()
            val currentState =
                _state.value.sheetState // basically the same exact behavior as without the non-critical error
            if (currentState is FileDetailHasFile) {
                _state.update { it.copy(sheetState = FileDetailLoading()) }
                fileService.updateFile(toUpdate).onSuccess { loadFile() }.onFailure { msg ->
                    _state.update {
                        it.copy(
                            sheetState = FileDetailMessage(
                                file = currentState.file,
                                folder = currentState.folder,
                                message = "Failed to update file: $msg",
                            ),
                        )
                    }
                    delay(5_000L)
                    _state.update {
                        it.copy(
                            sheetState = FileDetailLoaded(
                                file = currentState.file,
                                folder = currentState.folder,
                            ),
                        )
                    }
                }
            }
        }
}
