package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.TagApi
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import dev.ploiu.file_server_ui_new.model.FileRequest
import dev.ploiu.file_server_ui_new.service.FileService
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
}

class FileDetailLoading : FileDetailUiState
data class FileDetailLoaded(override val file: FileApi) : FileDetailUiState, FileDetailHasFile
class FileDeleted : FileDetailUiState

data class FileDetailErrored(val message: String) : FileDetailUiState
data class FileDetailMessage(override val file: FileApi, val message: String) : FileDetailUiState,
    FileDetailHasFile

data class FileDetailUiModel(
    val sheetState: FileDetailUiState,
)

// TODO this has potential to be pulled into common code (? - downloading a file would behave differently on android and desktop)
class FileDetailViewModel(
    private val fileService: FileService,
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
        fileRes
            .onSuccess { file -> _state.update { it.copy(sheetState = FileDetailLoaded(file)) } }
            .onFailure { msg -> _state.update { it.copy(sheetState = FileDetailErrored("Failed to load file: $msg")) } }
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
                                    message = msg
                                )
                            )
                        }
                    }
            } else {
                _state.update {
                    it.copy(
                        sheetState = FileDetailMessage(
                            file = currentState.file,
                            message = "The file name you passed does not match. Not deleting file."
                        )
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
            _state.update { it.copy(sheetState = FileDetailLoaded(currentState.file)) }
        }
    }

    fun downloadFile(saveLocation: PlatformFile) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FileDetailHasFile) {
            if (!saveLocation.exists() || !saveLocation.isDirectory()) {
                _state.update {
                    it.copy(sheetState = FileDetailMessage(currentState.file, "Selected directory does not exist"))
                }
            } else {
                fileService.downloadFile(currentState.file.id)
                    .onSuccess { res ->
                        val archiveName = currentState.file.name + ".tar"
                        Files.copy(res, saveLocation.file.toPath() / archiveName, StandardCopyOption.REPLACE_EXISTING)
                        res.close()
                    }
                    .onFailure { msg ->
                        _state.update {
                            it.copy(sheetState = FileDetailMessage(file = currentState.file, msg))
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
                            currentState.file,
                            "Failed to update file: $msg"
                        )
                    )
                }
                delay(5_000L)
                _state.update { it.copy(sheetState = FileDetailLoaded(currentState.file)) }
            }
        }
    }
}
