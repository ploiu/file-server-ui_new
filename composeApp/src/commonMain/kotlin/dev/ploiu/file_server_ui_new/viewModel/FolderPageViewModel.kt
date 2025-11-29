package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.saveFile
import dev.ploiu.file_server_ui_new.service.FolderService
import dev.ploiu.file_server_ui_new.service.PreviewService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.resolve
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.div

@Serializable
data class FolderRoute(val id: Long)

sealed interface FolderPageUiState
class FolderPageLoading : FolderPageUiState
data class FolderPageLoaded(val folder: FolderApi) : FolderPageUiState
data class FolderPageError(val message: String) : FolderPageUiState

data class FolderPageUiModel(
    val pageState: FolderPageUiState,
    val previews: BatchFilePreview,
    /** for non-critical messages that show as a snackbar. Not part of a "HasFolder" state because it's just simpler to do it this way since it doesn't impact main page state */
    val message: String? = null,
)

class FolderPageViewModel(
    private val folderService: FolderService,
    private val previewService: PreviewService,
    val folderId: Long,
) : ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(FolderPageUiModel(FolderPageLoading(), emptyMap()))
    val state = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
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
            previewService.getFolderPreview(folder).onSuccess { previews ->
                _state.update { it.copy(previews = previews) }
            }
        }.onFailure { error ->
            _state.update { it.copy(pageState = FolderPageError(error)) }
            log.error { "Failed to get folder information: $error" }
        }
    }


    /**
     * checks if the folder can be downloaded in the passed [saveLocation]. If so, the folder is downloaded. Otherwise,
     * the state is updated for the ui to signal a confirmation or error to the user
     */
    fun checkDownloadFolder(folder: FolderApi, saveLocation: PlatformFile): Boolean {
        return if (!saveLocation.exists() || !saveLocation.isDirectory()) {
            _state.update {
                it.copy(message = "Selected directory does not exist")
            }
            false
        } else {
            !saveLocation.resolve("${folder.name}.tar").exists()
        }
    }

    /**
     * Handles downloading the folder to the passed [saveLocation].
     *
     * This should only be called if [checkDownloadFolder] succeeds or if the user confirms they want to overwrite
     * */
    fun downloadFolder(folder: FolderApi, saveLocation: PlatformFile) =
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            folderService.downloadFolder(folder.id)
                .onSuccess { res ->
                    val archiveName = folder.name + ".tar"
                    saveFile(res, saveLocation, archiveName)
                    res.close()
                    _state.update { it.copy(message = "Folder downloaded successfully") }
                }
                .onFailure { msg ->
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
                loadFolder()
            }.onFailure { msg ->
                _state.update { it.copy(pageState = FolderPageError(msg)) }
            }
        }
    }

    /**
     * deletes the passed [folder] and calls [loadFolder] if successful.
     * If there's an error, the state is updated to have the error message
     */
    fun deleteFolder(folder: FolderApi) = viewModelScope.launch(Dispatchers.IO) {
        folderService.deleteFolder(folder.id)
            .onSuccess {
                _state.update { it.copy(message = "Folder deleted successfully") }
                loadFolder()
            }
            .onFailure { msg ->
                _state.update { it.copy(message = msg) }
            }
    }
}
