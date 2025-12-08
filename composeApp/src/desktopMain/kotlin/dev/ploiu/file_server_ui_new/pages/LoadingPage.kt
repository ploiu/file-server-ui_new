package dev.ploiu.file_server_ui_new.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import dev.ploiu.file_server_ui_new.components.dialog.Dialog
import dev.ploiu.file_server_ui_new.service.ErrorResult
import dev.ploiu.file_server_ui_new.service.IncompatibleResult
import dev.ploiu.file_server_ui_new.viewModel.LoadingPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.LoadingUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoadingPage(model: LoadingPageViewModel = koinViewModel(), onSuccess: () -> Unit) {
    val (pageState, checkResult) = model.state.collectAsState().value

    LaunchedEffect(Unit) {
        model.versionCheck()
    }

    when (pageState) {
        LoadingUiState.LOADING -> CircularProgressIndicator()
        LoadingUiState.SHOWING_MODAL -> {
            when (checkResult) {
                is ErrorResult -> Dialog(
                    title = "An Error Occurred",
                    text = checkResult.error.message,
                    icon = Icons.Default.Error,
                    iconColor = MaterialTheme.colorScheme.error,
                )

                is IncompatibleResult -> Dialog(
                    title = "Incompatible Server Version",
                    text = "The server is on version ${checkResult.serverVersion}, but this client only supports ${checkResult.compatibleVersion}",
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.secondary,
                )

                else -> println("bad check result on SHOWING_MODAL state!")
            }
        }

        LoadingUiState.NAVIGATING -> onSuccess()
    }
}
