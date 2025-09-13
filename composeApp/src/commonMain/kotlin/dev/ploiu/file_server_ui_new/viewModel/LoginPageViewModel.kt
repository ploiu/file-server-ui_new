package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.annotation.UnsafeResultErrorAccess
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.module.GLOBAL
import dev.ploiu.file_server_ui_new.service.ApiService
import dev.ploiu.file_server_ui_new.service.NoCredsFound
import dev.ploiu.file_server_ui_new.service.RetrieveCredsError
import dev.ploiu.file_server_ui_new.service.RetrieveCredsSuccess
import dev.ploiu.file_server_ui_new.service.retrieveCreds
import dev.ploiu.file_server_ui_new.service.saveCreds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
class LoginRoute

/** because of differences in mobile vs desktop, prompting for saving
 * creds needs to be controlled in the platform-specific composable */
sealed interface LoginPageState

/** nothing special on the UI */
class LoginUntried : LoginPageState

/** loading while testing creds */
class LoginLoading : LoginPageState

/** succeeded in logging in */
class LoginSuccess : LoginPageState

data class LoginError(val message: String) : LoginPageState

data class LoginPageUiModel(val pageState: LoginPageState, val savePassword: Boolean)

class LoginPageViewModel(
    val apiService: ApiService,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginPageUiModel(LoginUntried(), false))
    val state = _state.asStateFlow()

    fun setSavePassword(savePassword: Boolean) = _state.update { it.copy(savePassword = savePassword) }

    /**
     * attempts to retrieve the user's creds from the OS cred store. If they exist, they are tested
     * against the server. If they are accepted, then it signals to the composable that navigation should occur
     * If not, it signals to the composable that the user needs to be prompted for the credentials:
     *
     * - nothing is set if no creds are found
     * - pageState changes to a [LoginError] with an error message explaining what happened if retrieving the creds fails
     * - loginResult is set to [LoginResult.SUCCESS] if the creds were retrieved
     * - loginResult is set to [LoginResult.FAIL] if the creds were retrieved but are rejected by the server
     */
    fun attemptAutoLogin() = viewModelScope.launch(Dispatchers.IO) {
        // first step is attempt to load any stored creds
        when (val creds = retrieveCreds()) {
            is NoCredsFound -> {
                // do nothing - the user never saved creds so we can't auto login
                return@launch
            }

            is RetrieveCredsError -> {
                this@LoginPageViewModel._state.update { it.copy(pageState = LoginError(creds.message)) }
            }

            is RetrieveCredsSuccess -> {
                GLOBAL.username = creds.username
                GLOBAL.password = creds.password
                apiService.authenticatedPing()
                    .onSuccess {
                        this@LoginPageViewModel._state.update {
                            it.copy(
                                pageState = LoginSuccess(),
                            )
                        }
                    }
                    .onFailure {
                        this@LoginPageViewModel._state.update {
                            it.copy(
                                pageState = LoginError("Auto sign in failed: Invalid credentials"),
                            )
                        }
                    }
            }
        }
    }

    fun attemptManualLogin(username: String, password: String) = viewModelScope.launch(Dispatchers.IO) {
        GLOBAL.username = username
        GLOBAL.password = password
        apiService.authenticatedPing()
            .onSuccess {
                this@LoginPageViewModel._state.update {
                    it.copy(
                        pageState = LoginSuccess(),
                    )
                }
            }
            .onFailure {
                this@LoginPageViewModel._state.update {
                    it.copy(
                        pageState = LoginError("Sign in failed: Invalid credentials"),
                    )
                }
            }
    }

    /**
     * should be called after creds are tested via _manual_ login and the user has indicated that they want to save their creds
     */
    fun savePassword(username: String, password: String) {
        saveCreds(username, password)
    }
}

