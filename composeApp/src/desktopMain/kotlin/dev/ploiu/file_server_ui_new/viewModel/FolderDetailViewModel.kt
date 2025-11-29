package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.TagApi
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import dev.ploiu.file_server_ui_new.service.FolderService
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

/** controls the display of the entire folder detail view sheet itself */
sealed interface FolderDetailUiState

/** used for when a folder is renderable on the ui */
sealed interface FolderDetailHasFolder {
    val folder: FolderApi
}

class FolderDetailLoading : FolderDetailUiState
data class FolderDetailLoaded(override val folder: FolderApi) : FolderDetailUiState, FolderDetailHasFolder
class FolderDeleted : FolderDetailUiState

data class FolderDetailErrored(val message: String) : FolderDetailUiState
data class FolderDetailMessage(override val folder: FolderApi, val message: String) : FolderDetailUiState,
    FolderDetailHasFolder

data class FolderDetailUiModel(
    val sheetState: FolderDetailUiState,
)

// TODO this has potential to be pulled into common code (? - downloading a folder would behave differently on android and desktop)
class FolderDetailViewModel(
    private val folderService: FolderService,
    val folderId: Long,
) : ViewModel() {
    private val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
        _state.update {
            it.copy(
                sheetState = FolderDetailErrored(
                    "Failed to process folder information: " + (throwable.message ?: throwable.javaClass)
                )
            )
        }
    }
    private val _state = MutableStateFlow(FolderDetailUiModel(FolderDetailLoading()))
    val state = _state.asStateFlow()

    fun loadFolder() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        _state.update { it.copy(sheetState = FolderDetailLoading()) }
        val folderRes = folderService.getFolder(folderId)
        folderRes
            .onSuccess { folder -> _state.update { it.copy(sheetState = FolderDetailLoaded(folder)) } }
            .onFailure { msg -> _state.update { it.copy(sheetState = FolderDetailErrored("Failed to load folder: $msg")) } }
    }

    fun renameFolder(newName: String) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FolderDetailLoaded) {
            val toUpdate = currentState.folder.copy(name = newName).toUpdateFolder()
            updateFolder(toUpdate)
        }
    }

    fun deleteFolder(confirmText: String) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FolderDetailHasFolder) {
            val folderName = currentState.folder.name.lowercase().trim()
            if (confirmText.lowercase().trim() == folderName) {
                folderService.deleteFolder(currentState.folder.id)
                    .onSuccess {
                        _state.update { it.copy(sheetState = FolderDeleted()) }
                    }
                    .onFailure { msg ->
                        _state.update {
                            it.copy(
                                sheetState = FolderDetailMessage(
                                    folder = currentState.folder,
                                    message = msg
                                )
                            )
                        }
                    }
            } else {
                _state.update {
                    it.copy(
                        sheetState = FolderDetailMessage(
                            folder = currentState.folder,
                            message = "The folder name you passed does not match. Not deleting folder."
                        )
                    )
                }
            }
        }
    }

    fun updateTags(tags: Collection<TaggedItemApi>) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FolderDetailHasFolder) {
            val toUpdate = currentState.folder.copy(tags = tags).toUpdateFolder()
            updateFolder(toUpdate)
        }
    }

    fun clearNonCriticalError() {
        val currentState = _state.value.sheetState
        if (currentState is FolderDetailMessage) {
            _state.update { it.copy(sheetState = FolderDetailLoaded(currentState.folder)) }
        }
    }

    fun downloadFolder(saveLocation: PlatformFile) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        val currentState = _state.value.sheetState
        if (currentState is FolderDetailHasFolder) {
            if (!saveLocation.exists() || !saveLocation.isDirectory()) {
                _state.update {
                    it.copy(sheetState = FolderDetailMessage(currentState.folder, "Selected directory does not exist"))
                }
            } else {
                folderService.downloadFolder(currentState.folder.id)
                    .onSuccess { res ->
                        val archiveName = currentState.folder.name + ".tar"
                        Files.copy(res, saveLocation.file.toPath() / archiveName, StandardCopyOption.REPLACE_EXISTING)
                        res.close()
                    }
                    .onFailure { msg ->
                        _state.update {
                            it.copy(sheetState = FolderDetailMessage(folder = currentState.folder, msg))
                        }
                    }
            }
        }
    }

    private fun updateFolder(toUpdate: UpdateFolder) = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        // this is nice just in case
        clearNonCriticalError()
        val currentState = _state.value.sheetState
        // basically the same exact behavior as without the non-critical error
        if (currentState is FolderDetailHasFolder) {
            _state.update { it.copy(sheetState = FolderDetailLoading()) }
            folderService.updateFolder(toUpdate).onSuccess { loadFolder() }.onFailure { msg ->
                _state.update {
                    it.copy(
                        sheetState = FolderDetailMessage(
                            currentState.folder,
                            "Failed to update folder: $msg"
                        )
                    )
                }
                delay(5_000L)
                _state.update { it.copy(sheetState = FolderDetailLoaded(currentState.folder)) }
            }
        }
    }
}
