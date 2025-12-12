package dev.ploiu.file_server_ui_new

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ploiu.file_server_ui_new.MessageTypes.FOCUS_SEARCHBAR
import dev.ploiu.file_server_ui_new.MessageTypes.HIDE_ACTIVE_ELEMENT
import dev.ploiu.file_server_ui_new.components.AppHeader
import dev.ploiu.file_server_ui_new.components.NavBar
import dev.ploiu.file_server_ui_new.components.NavState
import dev.ploiu.file_server_ui_new.components.dialog.CurrentDialog
import dev.ploiu.file_server_ui_new.components.sidesheet.FileDetailSheet
import dev.ploiu.file_server_ui_new.components.sidesheet.FolderDetailSheet
import dev.ploiu.file_server_ui_new.components.sidesheet.StandardSideSheet
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
import dev.ploiu.file_server_ui_new.module.miscModule
import dev.ploiu.file_server_ui_new.module.serviceModule
import dev.ploiu.file_server_ui_new.pages.FolderPage
import dev.ploiu.file_server_ui_new.pages.LoadingPage
import dev.ploiu.file_server_ui_new.pages.LoginPage
import dev.ploiu.file_server_ui_new.pages.SearchResultsPage
import dev.ploiu.file_server_ui_new.ui.theme.darkScheme
import dev.ploiu.file_server_ui_new.ui.theme.lightScheme
import dev.ploiu.file_server_ui_new.viewModel.*
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.isDirectory
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
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
                modifier = Modifier.fillMaxSize(),
            ) {
                content()
            }
        }
    }
}


fun main() = application {
    try {
        startKoin {
            modules(configModule, clientModule, serviceModule, pageModule, desktopServiceModule, miscModule)
        }
    } catch (_: Exception) {
        println("Koin already started")
    }
    FileKit.init(appId = "PloiuFileServer")
    val messagePasser = remember { ObservableMessagePasser() }
    // TODO window breakpoints (jetbrains has a lib, see https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-adaptive-layouts.html)
    Window(
        onCloseRequest = ::exitApplication,
        title = "file-server-ui_new",
        state = rememberWindowState(width = 1200.dp, height = 600.dp),
        onKeyEvent = {
            when (it.key) {
                Key.K -> {
                    if (it.isCtrlPressed) {
                        messagePasser.passMessage(FOCUS_SEARCHBAR)
                    }
                }

                Key.Escape -> messagePasser.passMessage(HIDE_ACTIVE_ELEMENT)

                else -> {}
            }
            true
        },
    ) {
        AppTheme {
            MainDesktopBody(appViewModel = koinInject(), messagePasser = messagePasser)
        }
    }
}

// some routes shouldn't have the app header show up
val headerlessRoutes = listOf(
    LoginRoute::class.qualifiedName,
    LoadingRoute::class.qualifiedName,
)

private fun isHeaderless(route: String?) = headerlessRoutes.contains(route)

@Composable
fun MainDesktopBody(
    navController: NavHostController = rememberNavController(),
    appViewModel: ApplicationViewModel,
    messagePasser: ObservableMessagePasser,
    modalController: ModalController = koinViewModel(),
) {
    var navBarState: NavState by remember {
        mutableStateOf(
            NavState(
                LinkedList<FolderApi>(
                    // using a fake representation of the root folder here so that we don't have to break structure to pull it. Also since it's just for nav, it doesn't matter if we don't have the children here
                    listOf(
                        FolderApi(
                            0, null, "root", "~", emptyList(), emptyList(), emptyList(),
                        ),
                    ),
                ),
            ),
        )
    }
    val appState = appViewModel.state.collectAsState().value
    val searchBarFocuser = remember { FocusRequester() }
    val mainContentWidth =
        animateFloatAsState(
            targetValue = if (appState.sideSheetState is NoSideSheet) 1f else .7f,
            animationSpec = tween(),
        )
    // because the side sheet can update folders and files, we need a way to tell the folder page when to refresh.
    // This is lifted up and used to make FolderPage refresh its data when needed
    // TODO("figure out folder update key issues. I'd rather have just 1 but it seems like the side sheet doesn't close when the folder is deleted if that's the case - something to do with a rendering race condition? idk")
    val sideSheetOpacity = animateFloatAsState(targetValue = if (appState.sideSheetState is NoSideSheet) 0f else 1f)
    // same as sideSheetUpdateKey, but for changes originating from FolderPage
    var shouldShowHeader by remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value
    // for uploading folders
    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        if (directory?.isDirectory() ?: false && currentRoute?.destination?.route?.contains(
                FolderRoute::class.simpleName!!,
            ) ?: false
        ) {
            val folderId = currentRoute.toRoute<FolderRoute>().id
            appViewModel.uploadFolder(directory, folderId)
        }
    }

    LaunchedEffect(Unit) {
        messagePasser handles FOCUS_SEARCHBAR { searchBarFocuser.requestFocus() }
        messagePasser handles HIDE_ACTIVE_ELEMENT {
            // we're using this instead of the state variable because at this point, the state hasn't updated yet and we need a live version
            if (!modalController.isJustClosed) {
                appViewModel.closeSideSheet()
            } else {
                // don't close anything, but be sure to reset the dialog just closed flag
                modalController.clearJustClosed()
            }
        }
    }

    LaunchedEffect(currentRoute) {
        shouldShowHeader = !isHeaderless(currentRoute?.destination?.route)
    }

    Row {
        Column(modifier = Modifier.animateContentSize().weight(mainContentWidth.value, true)) {
            if (shouldShowHeader) {
                AppHeader(
                    searchBarFocuser = searchBarFocuser,
                    navController = navController,
                    sideSheetActive = appState.sideSheetState !is NoSideSheet,
                    onCreateFolderClick = appViewModel::openCreateEmptyFolderModal,
                    onUploadFolderClick = directoryPicker::launch,
                )
                Spacer(Modifier.height(8.dp))
                NavBar(state = navBarState) { folders ->
                    navBarState = navBarState.copy(folders = folders)
                    navController.navigate(FolderRoute(folders.last().id))
                }
                Spacer(Modifier.height(8.dp))
            }
            // stuff that changes
            NavHost(navController = navController, startDestination = LoginRoute()) {
                // TODO about page with open source licenses (probably located in settings page) - check fonts, icons
                composable<LoginRoute> {
                    LoginPage(navController = navController)
                }
                // TODO remove?
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
                        refreshKey = appState.updateKey,
                        onFolderInfo = { appViewModel.sideSheetItem(it) },
                        onFileInfo = { appViewModel.sideSheetItem(it) },
                        onUpdate = appViewModel::changeUpdateKey,
                    ) {
                        navController.navigate(FolderRoute(it.id))
                        navBarState += it
                    }
                }
                composable<SearchResultsRoute> { backStack ->
                    val route: SearchResultsRoute = backStack.toRoute()
                    val viewModel =
                        koinInject<SearchResultsPageViewModel> { parametersOf(route.searchTerm) }
                    SearchResultsPage(viewModel)
                }
            }
            CurrentDialog()
        }
        StandardSideSheet(
            modifier = Modifier.weight(1.01f - mainContentWidth.value, true).alpha(sideSheetOpacity.value),
            onCloseAction = appViewModel::closeSideSheet,
        ) {
            when (val sideSheetState = appState.sideSheetState) {
                is FolderSideSheet -> {
                    val viewModel = koinInject<FolderDetailViewModel> { parametersOf(sideSheetState.folder.id) }
                    FolderDetailSheet(
                        viewModel = viewModel,
                        closeSelf = { appViewModel.sideSheetItem(null) },
                        refreshKey = appState.updateKey,
                        onChange = appViewModel::changeUpdateKey,
                    )
                }

                is FileSideSheet -> {
                    val viewModel = koinInject<FileDetailViewModel> { parametersOf(sideSheetState.file.id) }
                    FileDetailSheet(
                        viewModel = viewModel,
                        closeSelf = { appViewModel.sideSheetItem(null) },
                        refreshKey = appState.updateKey,
                        onChange = appViewModel::changeUpdateKey,
                    )
                }

                is NoSideSheet -> {}
            }
        }
    }
}
