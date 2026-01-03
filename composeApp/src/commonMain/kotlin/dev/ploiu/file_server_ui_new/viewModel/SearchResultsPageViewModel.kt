package dev.ploiu.file_server_ui_new.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.*
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.PreviewService
import dev.ploiu.file_server_ui_new.util.getCachedFile
import dev.ploiu.file_server_ui_new.util.writeTempFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.ext.getFullName

@Serializable
class SearchResultsRoute(val searchTerm: String)

sealed interface SearchResultsUiState
class SearchResultsLoading : SearchResultsUiState
data class SearchResultsLoaded(val files: List<FileApi>, val previews: BatchFilePreview) : SearchResultsUiState

class SearchResultsError(val message: String) : SearchResultsUiState


data class SearchResultsPageUiModel(
    val pageState: SearchResultsUiState, val searchTerm: String,
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

    fun performSearch() = viewModelScope.launch(Dispatchers.IO) {
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
}
