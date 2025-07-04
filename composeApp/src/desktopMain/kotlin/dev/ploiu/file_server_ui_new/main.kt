package dev.ploiu.file_server_ui_new

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ploiu.file_server_ui_new.components.FileServerSearchBar
import dev.ploiu.file_server_ui_new.components.NavBar
import dev.ploiu.file_server_ui_new.components.NavState
import dev.ploiu.file_server_ui_new.components.StandardSideSheet
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
import dev.ploiu.file_server_ui_new.module.serviceModule
import dev.ploiu.file_server_ui_new.pages.FolderPage
import dev.ploiu.file_server_ui_new.pages.LoadingPage
import dev.ploiu.file_server_ui_new.pages.SearchResultsPage
import dev.ploiu.file_server_ui_new.ui.theme.darkScheme
import dev.ploiu.file_server_ui_new.ui.theme.lightScheme
import dev.ploiu.file_server_ui_new.viewModel.*
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import java.util.*

@Composable
actual fun AppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        val contextMenuRepresentation = if (isSystemInDarkTheme()) {
            DarkDefaultContextMenuRepresentation
        } else {
            LightDefaultContextMenuRepresentation
        }
        CompositionLocalProvider(LocalContextMenuRepresentation provides contextMenuRepresentation) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        }
    }
}


fun main() = application {
    // TODO move configModule to desktopMain and separate out from commonMain - desktops are less likely to be shared,
    //  and I want the user to type username + password on the android version since those devices are more easily losable
    startKoin {
        modules(configModule, clientModule, serviceModule, pageModule, desktopServiceModule)
    }
    val searchBarFocuser = remember { FocusRequester() }
    // TODO window breakpoints (jetbrains has a lib, see https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-adaptive-layouts.html)
    Window(
        onCloseRequest = ::exitApplication,
        title = "file-server-ui_new",
        state = rememberWindowState(width = 1200.dp, height = 600.dp),
        onKeyEvent = {
            when (it.key) {
                Key.K -> {
                    if (it.isCtrlPressed) {
                        searchBarFocuser.requestFocus()
                    }
                }

                else -> {}
            }
            true
        }
    ) {
        AppTheme {
            NavigationHost(searchBarFocuser = searchBarFocuser)
        }
    }
}

@Composable
fun NavigationHost(
    navController: NavHostController = rememberNavController(),
    searchBarFocuser: FocusRequester,
) {
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
    /* this is a generic container for subcomponents to push whatever view they want to it. This
    breaks the unidirectional data flow model for compose, but it allows us to keep this
    generic and allows all the necessary logic to live in the child component tightly
    coupled with the data. (e.g. showing folder / file info - having to hard code interactions
    for that here would get messy fast) */
    var sideSheetContents: @Composable (() -> Unit)? by remember { mutableStateOf(null) }
    val mainContentWidth =
        animateFloatAsState(targetValue = if (sideSheetContents == null) 1f else .7f, animationSpec = tween())
    val sideSheetOpacity = animateFloatAsState(targetValue = if (sideSheetContents == null) 0f else 1f)
    val searchBarSize = animateFloatAsState(if (sideSheetContents == null) .8f else 1f, animationSpec = tween())

    Row {
        Column(modifier = Modifier.animateContentSize().weight(mainContentWidth.value, true)) {
            // top level components that should show on every view
            FileServerSearchBar(
                focusRequester = searchBarFocuser,
                modifier = Modifier.fillMaxWidth(searchBarSize.value)
            ) {
                navController.navigate(SearchResultsRoute(it))
            }
            NavBar(navBarState) { folder ->
                val index = navBarState.folders.indexOfFirst { it.id == folder.id }
                if (index != -1) {
                    val newFolders = navBarState.folders.subList(0, index + 1)
                    navBarState = navBarState.copy(folders = LinkedList<FolderApi>(newFolders))
                }
                navController.navigate(FolderRoute(folder.id))
            }
            // stuff that changes
            NavHost(navController = navController, startDestination = LoadingRoute()) {
                composable<LoadingRoute> {
                    LoadingPage {
                        navController.navigate(FolderRoute(0))
                    }
                }
                composable<FolderRoute> { backStack ->
                    val route: FolderRoute = backStack.toRoute()
                    val viewModel = koinInject<FolderPageViewModel> { parametersOf(route.id) }
                    FolderPage(view = viewModel, populateSideSheet = { sideSheetContents = it }) {
                        navController.navigate(FolderRoute(it.id))
                        val newFolders = navBarState.folders.toMutableList()
                        newFolders.add(it)
                        navBarState = navBarState.copy(folders = LinkedList<FolderApi>(newFolders))
                    }
                }
                composable<SearchResultsRoute> { backStack ->
                    val route: SearchResultsRoute = backStack.toRoute()
                    val viewModel =
                        koinInject<SearchResultsPageViewModel> { parametersOf(route.searchTerm) }
                    SearchResultsPage(viewModel)
                }
            }
        }
        StandardSideSheet(
            "test side sheet",
            modifier = Modifier.weight(1.01f - mainContentWidth.value, true).alpha(sideSheetOpacity.value),
            onCloseAction = { sideSheetContents = null }) {
            if (sideSheetContents != null) {
                sideSheetContents!!.invoke()
            }
        }
    }
}
