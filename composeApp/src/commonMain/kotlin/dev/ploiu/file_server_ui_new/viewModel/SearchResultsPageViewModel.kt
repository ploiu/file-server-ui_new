package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.PreviewService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
class SearchResultsRoute(val searchTerm: String)

sealed interface SearchResultsUiState
class SearchResultsLoading : SearchResultsUiState
class SearchResultsLoaded(val files: Collection<FileApi>) : SearchResultsUiState
class SearchResultsError(val message: String) : SearchResultsUiState


data class SearchResultsPageUiModel(
    val pageState: SearchResultsUiState, val searchTerm: String
)

class SearchResultsPageViewModel(
    val fileService: FileService,
    val previewService: PreviewService,
    val searchTerm: String
) : ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(SearchResultsPageUiModel(SearchResultsLoading(), searchTerm))
    val state = _state.asStateFlow()

    fun performSearch() = viewModelScope.launch(Dispatchers.IO) {
        val res = fileService.search(searchTerm)
        res.onSuccess { files ->
            _state.update { it.copy(pageState = SearchResultsLoaded(files)) }
            // TODO download previews. new method that takes a collection of file IDs and internally batches them to the server. Put in PreviewService
        }.onFailure { message ->
            _state.update { it.copy(pageState = SearchResultsError(message)) }
        }
    }
}
