package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.ploiu.file_server_ui_new.viewModel.FolderRoute
import dev.ploiu.file_server_ui_new.viewModel.LoginError
import dev.ploiu.file_server_ui_new.viewModel.LoginPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.LoginSuccess
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginPage(viewModel: LoginPageViewModel = koinInject(), navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val (pageState, shouldSavePassword) = viewModel.state.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.attemptAutoLogin()
    }

    LaunchedEffect(pageState) {
        println(pageState)
        if (pageState is LoginSuccess) {
            if (shouldSavePassword) {
                viewModel.savePassword(username, password)
            }
            navController.navigate(FolderRoute(0))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Sign in", style = MaterialTheme.typography.headlineLarge)
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
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (pageState is LoginError) {
            Text(text = pageState.message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = shouldSavePassword, onCheckedChange = { viewModel.setSavePassword(it) })
            Text(
                text = "Remember Me",
                modifier = Modifier.onClick(onClick = { viewModel.setSavePassword(!shouldSavePassword) })
            )
        }
        Button(onClick = {
            if (username.isNotBlank() && password.isNotBlank()) {
                viewModel.attemptManualLogin(username, password)
            }
        }) {
            Text("Sign in")
        }
    }
}
