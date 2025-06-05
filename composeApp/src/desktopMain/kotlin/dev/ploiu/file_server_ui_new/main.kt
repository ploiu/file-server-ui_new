package dev.ploiu.file_server_ui_new

import androidx.compose.material3.MaterialTheme
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
import dev.ploiu.file_server_ui_new.ui.theme.darkScheme
import dev.ploiu.file_server_ui_new.views.FolderList
import dev.ploiu.file_server_ui_new.views.FolderRoute
import dev.ploiu.file_server_ui_new.views.FolderView
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf

@Composable
actual fun ShowPlatformView() = NavigationHost()

@Composable
actual fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = darkScheme) {
        content()
    }
}


fun main() = application {
    // TODO move configModule to desktopMain and separate out from commonMain - desktops are less likely to be shared, and I want the user to type username + password on the android version
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

@Composable
fun NavigationHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = FolderRoute(0)) {
        composable<FolderRoute> { backStack ->
            val route: FolderRoute = backStack.toRoute()
            val viewModel: FolderView = koinInject<FolderView> { parametersOf(route.id) }
            FolderList(viewModel) { navController.navigate(FolderRoute(it.id)) }
        }
    }
}
