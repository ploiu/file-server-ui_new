package dev.ploiu.file_server_ui_new

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
import dev.ploiu.file_server_ui_new.module.serviceModule
import dev.ploiu.file_server_ui_new.views.FolderView
import dev.ploiu.file_server_ui_new.views.LoadingScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.*
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

@Composable
actual fun ShowPlatformView() = NavigationHost()

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

// TODO refactor out
@Serializable
data class FolderRoute(val id: Long)

@Composable
fun NavigationHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = FolderRoute(0)) {
        composable<FolderRoute> {backStack ->
            val route: FolderRoute = backStack.toRoute()
            val viewModel: FolderView = koinInject<FolderView> { parametersOf(route.id) }
            Test(viewModel)
        }
    }
}

@Composable
fun Test(view: FolderView) {
    Text(view.toString())
}
