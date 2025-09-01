package dev.ploiu.file_server_ui_new.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.ploiu.file_server_ui_new.client.ApiClient
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Composable
fun LoginPage(client: ApiClient = koinInject<ApiClient>(), navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // TODO("fields, button, ask to save password, config, nav on successful password")
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
        Button(onClick = {}) {
            Text("Sign in")
        }
    }
}
