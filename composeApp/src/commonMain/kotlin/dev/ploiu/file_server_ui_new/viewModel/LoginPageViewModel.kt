package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.annotation.UnsafeResultErrorAccess
import dev.ploiu.file_server_ui_new.module.GLOBAL
import dev.ploiu.file_server_ui_new.service.ApiService
import dev.ploiu.file_server_ui_new.service.NoCredsFound
import dev.ploiu.file_server_ui_new.service.RetrieveCredsError
import dev.ploiu.file_server_ui_new.service.RetrieveCredsSuccess
import dev.ploiu.file_server_ui_new.service.retrieveCreds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
class LoginRoute

enum class LoginResult {
    UNTRIED,
    SUCCESS,
    FAIL
}

/** because of differences in mobile vs desktop, prompting for saving
 * creds needs to be controlled in the platform-specific composable */
sealed interface LoginPageState

/** nothing special on the UI */
class LoginDefault : LoginPageState

/** loading while testing creds */
class LoginLoading : LoginPageState

data class LoginError(val message: String) : LoginPageState


data class LoginPageUiModel(val pageState: LoginPageState, val loginResult: LoginResult)

class LoginPageViewModel(
    val apiService: ApiService,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginPageUiModel(LoginLoading(), LoginResult.UNTRIED))
    val state = _state.asStateFlow()

    /**
     * attempts to retrieve the user's creds from the OS cred store. If they exist, they are tested
     * against the server. If they are accepted, then it signals to the composable that navigation should occur
     * If not, it signals to the composable that the user needs to be prompted for the credentials
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
                TODO("creds have been set, call the storage info method with api service, and check if login succeeded. If so, set appropriate state for navigation")
            }
        }
    }

    fun attemptManualLogin() = viewModelScope.launch(Dispatchers.IO) {
        TODO("username and password as fields on this model (both mobile and desktop will use them so why not)")
    }
}

