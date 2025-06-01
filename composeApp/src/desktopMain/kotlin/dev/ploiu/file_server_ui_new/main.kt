package dev.ploiu.file_server_ui_new

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.ploiu.file_server_ui_new.module.DaggerAppComponent
import kotlinx.coroutines.launch

object Global {
    val appComponent = DaggerAppComponent.create()
    val desktopComponent: DaggerDesktopComponent.create()
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "file-server-ui_new",
    ) {
        App()
    }
}
