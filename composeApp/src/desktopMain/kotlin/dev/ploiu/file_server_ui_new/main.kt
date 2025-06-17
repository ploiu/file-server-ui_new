package dev.ploiu.file_server_ui_new

import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
import dev.ploiu.file_server_ui_new.module.serviceModule
import dev.ploiu.file_server_ui_new.ui.theme.darkScheme
import dev.ploiu.file_server_ui_new.ui.theme.lightScheme
import dev.ploiu.file_server_ui_new.views.*
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import java.util.*

@Composable
actual fun ShowPlatformView() = NavigationHost()

@Composable
actual fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        val contextMenuRepresentation = if (isSystemInDarkTheme()) {
            DarkDefaultContextMenuRepresentation
        } else {
            LightDefaultContextMenuRepresentation
        }
        CompositionLocalProvider(LocalContextMenuRepresentation provides contextMenuRepresentation) {
            content()
        }
    }
}


fun main() = application {
    // TODO move configModule to desktopMain and separate out from commonMain - desktops are less likely to be shared, and I want the user to type username + password on the android version
    startKoin {
        modules(configModule, clientModule, serviceModule, componentViewModule, desktopServiceModule)
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
    var navBarState: NavState by remember {
        mutableStateOf(
            NavState(
                LinkedList<FolderApi>(
                    listOf(
                        // using a fake representation of the root folder here so that we don't have to break structure to pull it.
                        // Also since it's just for nav, it doesn't matter if we don't have the children here
                        FolderApi(
                            0,
                            null,
                            "root",
                            "~",
                            emptyList(),
                            emptyList(),
                            emptyList()
                        )
                    )
                )
            )
        )
    }
    Column {
        NavBar(navBarState) { folder ->
            val index = navBarState.folders.indexOfFirst { it.id == folder.id }
            if (index != -1) {
                val newFolders = navBarState.folders.subList(0, index + 1)
                navBarState = navBarState.copy(folders = LinkedList<FolderApi>(newFolders))
            }
            navController.navigate(FolderRoute(folder.id))
        }
        NavHost(navController = navController, startDestination = LoadingRoute()) {
            composable<LoadingRoute> {
                LoadingScreen {
                    navController.navigate(FolderRoute(0))
                }
            }
            composable<FolderRoute> { backStack ->
                val route: FolderRoute = backStack.toRoute()
                val viewModel: FolderView = koinInject<FolderView> { parametersOf(route.id) }
                FolderList(viewModel) {
                    navController.navigate(FolderRoute(it.id))
                    val newFolders = navBarState.folders.toMutableList()
                    newFolders.add(it)
                    navBarState = navBarState.copy(folders = LinkedList<FolderApi>(newFolders))
                }
            }
        }
    }
}
