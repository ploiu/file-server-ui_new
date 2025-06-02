package dev.ploiu.file_server_ui_new

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
import dev.ploiu.file_server_ui_new.module.serviceModule
import dev.ploiu.file_server_ui_new.views.LoadingScreen
import org.koin.core.context.startKoin

@Composable
actual fun ShowPlatformView() = LoadingScreen()

fun main() = application {
    startKoin {
        modules(configModule, clientModule, serviceModule, componentViewModule)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "file-server-ui_new",
    ) {
        App()
    }
}
