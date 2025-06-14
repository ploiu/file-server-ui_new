package dev.ploiu.file_server_ui_new.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.components.Dialog
import dev.ploiu.file_server_ui_new.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
class LoadingRoute

enum class PageStates {
    /** running compatibility check */
    LOADING,

    /** either an exception is thrown or the server is incompatible */
    SHOWING_MODAL,

    /** compatibility result succeeded - navigating to next page */
    NAVIGATING
}

data class LoadingState(val pageState: PageStates, val checkResult: ServerCompatibilityResult?)

class LoadingView(var apiService: ApiService) : ViewModel() {
    private val _state = MutableStateFlow(LoadingState(PageStates.LOADING, null))
    val state: StateFlow<LoadingState> = _state.asStateFlow()

    fun versionCheck() = viewModelScope.launch {
        val compatibility = try {
            apiService.isCompatibleWithServer()
        } catch (e: Exception) {
            ErrorResult(e)
        }
        _state.update {
            val pageState = when (compatibility) {
                is CompatibleResult -> PageStates.NAVIGATING
                is IncompatibleResult -> PageStates.SHOWING_MODAL
                is ErrorResult -> PageStates.SHOWING_MODAL
            }
            LoadingState(pageState, compatibility)
        }
        compatibility
    }
}

@Composable
fun LoadingScreen(model: LoadingView = koinViewModel(), onSuccess: () -> Unit) {
    val (pageState, checkResult) = model.state.collectAsState().value

    LaunchedEffect(Unit) {
        model.versionCheck()
    }

    when (pageState) {
        PageStates.LOADING -> CircularProgressIndicator()
        PageStates.SHOWING_MODAL -> {
            when (checkResult) {
                is ErrorResult -> Dialog(
                    title = "An Error Occurred",
                    text = checkResult.error.message,
                    icon = Icons.Default.Error,
                    iconColor = MaterialTheme.colorScheme.error
                )

                is IncompatibleResult -> Dialog(
                    title = "Incompatible Server Version",
                    text = "The server is on version ${checkResult.serverVersion}, but this client only supports ${checkResult.compatibleVersion}",
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.secondary
                )

                else -> println("bad check result on SHOWING_MODAL state!")
            }
        }

        PageStates.NAVIGATING -> onSuccess()
    }
}
