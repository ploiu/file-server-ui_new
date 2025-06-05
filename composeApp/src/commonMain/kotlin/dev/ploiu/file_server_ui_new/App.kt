package dev.ploiu.file_server_ui_new

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ploiu.file_server_ui_new.ui.theme.backgroundDark
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
expect fun ShowPlatformView()
@Composable
expect fun AppTheme(
    content: @Composable () -> Unit
)

@Preview
@Composable
fun App() {
    AppTheme {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface {
                ShowPlatformView()
            }
        }
    }
}
