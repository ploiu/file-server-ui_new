package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import dev.ploiu.file_server_ui_new.service.ApiService
import kotlinx.serialization.Serializable

@Serializable
class LoginRoute

sealed interface LoginPageUiState
class LoginDefault: LoginPageUiState
class LoginInvalidCreds: LoginPageUiState
class LoginLoading: LoginPageUiState

data class LoginPageUiModel(
    val pageState: LoginPageUiState
)

class LoginPageViewModel(
    val apiService: ApiService,
): ViewModel() {
    // TODO use cred functions etc etc idfk
}

