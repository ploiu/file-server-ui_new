package dev.ploiu.file_server_ui_new

import androidx.compose.runtime.Composable


@Composable
expect fun AppTheme(
    content: @Composable () -> Unit
)

/* @Preview
@Composable
fun App() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface {
                ShowPlatformView()
            }
        }
    }
} */
