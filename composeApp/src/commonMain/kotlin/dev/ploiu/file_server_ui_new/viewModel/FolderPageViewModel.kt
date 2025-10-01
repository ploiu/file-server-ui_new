package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
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

sealed interface FolderUiState
class FolderLoading : FolderUiState
data class FolderLoaded(val folder: FolderApi) : FolderUiState
data class FolderError(val message: String) : FolderUiState

data class FolderPageUiModel(
    val pageState: FolderUiState,
    val previews: BatchFilePreview,
    /** if not null, an error dialog needs to show. Used for non-critical errors */
    val errorMessage: String? = "some message",
)

class FolderPageViewModel(
    val folderService: FolderService,
    val previewService: PreviewService,
    val folderId: Long,
) : ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(FolderPageUiModel(FolderLoading(), emptyMap()))
    val state = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
        _state.update {
            val errorMessage = throwable.message ?: "Unknown error: ${throwable.javaClass.name}"
            it.copy(errorMessage = errorMessage)
        }
    }

    fun loadFolder() = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
        _state.update { it.copy(pageState = FolderLoading()) }
        val folderRes = folderService.getFolder(folderId)
        folderRes.onSuccess { folder ->
            _state.update { it.copy(pageState = FolderLoaded(folder)) }
            previewService.getFolderPreview(folder).onSuccess { previews ->
                _state.update { it.copy(previews = previews) }
            }
        }.onFailure { error ->
            _state.update { it.copy(pageState = FolderError(error)) }
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
                it.copy(errorMessage = "Selected directory does not exist")
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
                    // TODO error / success snackbar
                    val archiveName = folder.name + ".tar"
                    Files.copy(res, saveLocation.file.toPath() / archiveName, StandardCopyOption.REPLACE_EXISTING)
                    res.close()
                }
                .onFailure { msg ->
                    _state.update {
                        it.copy(errorMessage = msg)
                    }
                }
        }

    /** clears out errorMessage */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun updateFolder(folder: FolderApi) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.value.pageState
        if (currentState is FolderLoaded) {
            _state.update { it.copy(pageState = FolderLoading()) }
            val updateRes = folderService.updateFolder(folder.toUpdateFolder())
            updateRes.onSuccess {
                // TODO going round trip to the server just to update 1 folder might be wasteful.
                //  Might be better to just update the folder in our existing model without
                //  having to pull again...will need refresh logic though (ctrl + r)
                loadFolder()
            }.onFailure { msg ->
                _state.update { it.copy(pageState = FolderError(msg)) }
            }
        }
    }

    /**
     * deletes the passed [folder] and calls [loadFolder] if successful.
     * If there's an error, the state is updated to have the error message
     */
    fun deleteFolder(folder: FolderApi) = viewModelScope.launch(Dispatchers.IO) {
        folderService.deleteFolder(folder.id)
            .onSuccess { loadFolder() }
            .onFailure { msg ->
                _state.update { it.copy(errorMessage = msg) }
            }
    }
}
