package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.*
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.PreviewService
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.div

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
    FileDetailUiState,
    FileDetailHasFile

data class FileDetailUiModel(
    val sheetState: FileDetailUiState,
)

// TODO this has potential to be pulled into common code (? - downloading a file would behave differently on android and desktop)
class FileDetailViewModel(
    private val fileService: FileService,
    private val folderService: FolderService,
    private val previewService: PreviewService,
    val fileId: Long,
) : ViewModel() {
    private val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
        _state.update {
            it.copy(
                sheetState = FileDetailErrored(
                    "Failed to process file information: " + (throwable.message ?: throwable.javaClass)
                )
            )
        }
    }
    private val _state = MutableStateFlow(FileDetailUiModel(FileDetailLoading()))
    val state = _state.asStateFlow()

    fun loadFile() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        _state.update { it.copy(sheetState = FileDetailLoading()) }
        val fileRes = fileService.getMetadata(fileId)
        fileRes.andThen { f -> folderService.getFolder(f.folderId).map { folder -> folder to f } }
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
            previewService.getFilePreviews(file)
                .onSuccess { previews ->
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

    fun deleteFile(confirmText: String) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            val fileName = currentState.file.name.lowercase().trim()
            if (confirmText.lowercase().trim() == fileName) {
                fileService.deleteFile(currentState.file.id)
                    .onSuccess {
                        _state.update { it.copy(sheetState = FileDeleted()) }
                    }
                    .onFailure { msg ->
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
            } else {
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            file = currentState.file,
                            folder = currentState.folder,
                            message = "The file name you passed does not match. Not deleting file.",
                        ),
                    )
                }
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

    fun downloadFile(saveLocation: PlatformFile) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            if (!saveLocation.exists() || !saveLocation.isDirectory()) {
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            folder = currentState.folder,
                            file = currentState.file,
                            message = "Selected directory does not exist",
                        ),
                    )
                }
            } else {
                fileService.getFileContents(currentState.file.id)
                    .onSuccess { res ->
                        val archiveName = currentState.file.name
                        Files.copy(res, saveLocation.file.toPath() / archiveName, StandardCopyOption.REPLACE_EXISTING)
                        res.close()
                    }
                    .onFailure { msg ->
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
    }

    private fun updateFile(toUpdate: FileRequest) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        // this is nice just in case
        clearNonCriticalError()
        val currentState = _state.value.sheetState
        // basically the same exact behavior as without the non-critical error
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
