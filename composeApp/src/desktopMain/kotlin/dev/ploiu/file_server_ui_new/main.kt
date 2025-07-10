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
import dev.ploiu.file_server_ui_new.components.*
import dev.ploiu.file_server_ui_new.model.FileApi
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

private sealed interface SideSheetStatus
private class NoContents : SideSheetStatus
private data class FolderSideSheet(val folder: FolderApi) : SideSheetStatus
private data class FileSideSheet(val file: FileApi) : SideSheetStatus

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
            MainDesktopBody(searchBarFocuser = searchBarFocuser)
        }
    }
}

@Composable
fun MainDesktopBody(
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
    var sideSheetStatus: SideSheetStatus by remember { mutableStateOf(NoContents()) }
    val mainContentWidth =
        animateFloatAsState(targetValue = if (sideSheetStatus is NoContents) 1f else .7f, animationSpec = tween())
    val sideSheetOpacity = animateFloatAsState(targetValue = if (sideSheetStatus is NoContents) 0f else 1f)
    val searchBarSize = animateFloatAsState(if (sideSheetStatus is NoContents) .8f else 1f, animationSpec = tween())
    // because the side sheet can update folders and files, we need a way to tell the folder page when to refresh. This is lifted up and used to make FolderPage refresh its data when needed
    var sideSheetUpdateKey: Int by remember { mutableStateOf(0) }
    // same as sideSheetUpdateKey, but for changes originating from FolderPage
    var folderPageUpdateKey: Int by remember { mutableStateOf(0) }

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
                // TODO about page with open source licenses (probably located in settings page) - check fonts, icons
                composable<LoadingRoute> {
                    LoadingPage {
                        navController.navigate(FolderRoute(0))
                    }
                }
                composable<FolderRoute> { backStack ->
                    val route: FolderRoute = backStack.toRoute()
                    val viewModel = koinInject<FolderPageViewModel> { parametersOf(route.id) }
                    FolderPage(
                        view = viewModel,
                        refreshKey = sideSheetUpdateKey,
                        onFolderInfo = { sideSheetStatus = FolderSideSheet(it) },
                        onUpdate = { folderPageUpdateKey += 1 }) {
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
        // FIXME updates from FolderPage won't reflect in side sheet
        StandardSideSheet(
            modifier = Modifier.weight(1.01f - mainContentWidth.value, true).alpha(sideSheetOpacity.value),
            onCloseAction = { sideSheetStatus = NoContents() }) {
            when (val currentSheet = sideSheetStatus) {
                is FolderSideSheet -> {
                    val viewModel = koinInject<FolderDetailViewModel> { parametersOf(currentSheet.folder.id) }
                    FolderDetailSheet(
                        viewModel = viewModel,
                        closeSelf = { sideSheetStatus = NoContents() },
                        refreshKey = folderPageUpdateKey
                    ) {
                        sideSheetUpdateKey += 1
                    }
                }

                is FileSideSheet -> TODO()
                is NoContents -> {}
            }
        }
    }
}
