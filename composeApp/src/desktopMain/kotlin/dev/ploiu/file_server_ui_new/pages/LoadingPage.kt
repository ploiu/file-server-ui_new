package dev.ploiu.file_server_ui_new.pages

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import dev.ploiu.file_server_ui_new.viewModel.LoadingPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.LoadingUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoadingPage(model: LoadingPageViewModel = koinViewModel(), onSuccess: () -> Unit) {
    val (pageState) = model.state.collectAsState().value

    LaunchedEffect(Unit) {
        model.versionCheck()
    }

    when (pageState) {
        LoadingUiState.LOADING -> CircularProgressIndicator()
        LoadingUiState.NAVIGATING -> onSuccess()
    }
}
