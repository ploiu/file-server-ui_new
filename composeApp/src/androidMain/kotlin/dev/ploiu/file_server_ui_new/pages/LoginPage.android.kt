package dev.ploiu.file_server_ui_new.pages

import androidx.biometric.AuthenticationRequest
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.ploiu.file_server_ui_new.storage.AppSettings
import dev.ploiu.file_server_ui_new.viewModel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun LoginPage(viewModel: LoginPageViewModel = koinInject(), navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // is the page trying to sign in automatically using saved credentials
    var isAutoSignin by remember { mutableStateOf(false) }

    val authLauncher = rememberAuthenticationLauncher { res ->
        if (res.isSuccess()) {
            if (isAutoSignin) {
                viewModel.attemptAutoLogin()
            } else {
                viewModel.savePassword(username = username, password = password)
            }
        }
    }
    val (pageState, shouldSavePassword) = viewModel.state.collectAsState().value

    LaunchedEffect(Unit) {
        val hasPassword = withContext(Dispatchers.IO) { AppSettings.doesHavePasswordSaved() }
        if (hasPassword) {
            isAutoSignin = true
            authLauncher.launch(
                AuthenticationRequest.biometricRequest(
                    title = "Use saved password",
                    authFallback = AuthenticationRequest.Biometric.Fallback.DeviceCredential,
                ) {
                    setMinStrength(AuthenticationRequest.Biometric.Strength.Class2)
                },
            )
        }
    }

    LaunchedEffect(pageState) {
        if (pageState is LoginSuccess) {
            if (shouldSavePassword) {
                authLauncher.launch(
                    input = AuthenticationRequest.biometricRequest(
                        title = "Confirm save password",
                        authFallback = AuthenticationRequest.Biometric.Fallback.DeviceCredential,
                    ) {
                        setMinStrength(AuthenticationRequest.Biometric.Strength.Class2)
                    },
                )
            }
            navController.navigate(FolderRoute(0))
        }
    }

    if (pageState is LoginLoading || pageState is LoginSuccess) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("username") },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("password") },
                        maxLines = 1,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    if (pageState is LoginError) {
                        Text(text = pageState.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = shouldSavePassword, onCheckedChange = { viewModel.setSavePassword(it) })
                        Text(
                            text = "Remember Me",
                            modifier = Modifier.clickable(onClick = { viewModel.setSavePassword(!shouldSavePassword) }),
                        )
                    }
                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                viewModel.attemptManualLogin(username, password)
                            }
                        },
                    ) {
                        Text("Sign in")
                    }
                }
            }
        }
    }
}
