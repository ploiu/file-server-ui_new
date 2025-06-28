package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.unwrap
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
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
data class SearchResultsLoaded(val files: List<FileApi>, val previews: BatchFilePreview) : SearchResultsUiState
class SearchResultsError(val message: String) : SearchResultsUiState


data class SearchResultsPageUiModel(
    val pageState: SearchResultsUiState, val searchTerm: String
)

class SearchResultsPageViewModel(
    val fileService: FileService, val previewService: PreviewService, val searchTerm: String
) : ViewModel() {
    private val log = KotlinLogging.logger { }
    private val _state = MutableStateFlow(SearchResultsPageUiModel(SearchResultsLoading(), searchTerm))
    val state = _state.asStateFlow()

    fun performSearch() = viewModelScope.launch(Dispatchers.IO) {
        val res = fileService.search(searchTerm)
        res.onSuccess { files ->
            val sorted = files.toList()
                .sortedWith(compareBy<FileApi> { it.name.lowercase() }.thenBy { it.dateCreated }.thenBy { it.id })
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
}
