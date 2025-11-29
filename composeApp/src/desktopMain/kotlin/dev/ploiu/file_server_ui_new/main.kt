package dev.ploiu.file_server_ui_new

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ploiu.file_server_ui_new.components.*
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
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
    try {
        startKoin {
            modules(configModule, clientModule, serviceModule, pageModule, desktopServiceModule)
        }
    } catch (_: Exception) {
        println("Koin already started")
    }
    FileKit.init(appId = "PloiuFileServer")
    val searchBarFocuser = remember { FocusRequester() }
    val viewModel = koinInject<ApplicationViewModel>()
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
            MainDesktopBody(searchBarFocuser = searchBarFocuser, appViewModel = viewModel)
        }
    }
}

// some routes shouldn't have the app header show up
val headerlessRoutes = listOf(
    LoginRoute::class.qualifiedName,
    LoadingRoute::class.qualifiedName
)

private fun isHeaderless(route: String?) = headerlessRoutes.contains(route)


// TODO pull this out into its own file (probably same with other modals)
@Composable
fun LoadingModalDialog(loadingModal: LoadingModal) {
    Dialog(onDismissRequest = {}) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { loadingModal.progress / loadingModal.max.toFloat() },
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Uploading... ${loadingModal.progress} / ${loadingModal.max}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun MainDesktopBody(
    navController: NavHostController = rememberNavController(),
    searchBarFocuser: FocusRequester,
    appViewModel: ApplicationViewModel
) {
    var navBarState: NavState by remember {
        mutableStateOf(
            NavState(
                LinkedList<FolderApi>(
                    // using a fake representation of the root folder here so that we don't have to break structure to pull it. Also since it's just for nav, it doesn't matter if we don't have the children here
                    listOf(
                        FolderApi(
                            0, null, "root", "~", emptyList(), emptyList(), emptyList()
                        )
                    )
                )
            )
        )
    }
    val (sideSheetStatus, headerUpdateKey) = appViewModel.state.collectAsState().value
    val modalState = appViewModel.modalState.collectAsState().value
    val mainContentWidth =
        animateFloatAsState(targetValue = if (sideSheetStatus is NoSideSheet) 1f else .7f, animationSpec = tween())
    // because the side sheet can update folders and files, we need a way to tell the folder page when to refresh.
    // This is lifted up and used to make FolderPage refresh its data when needed
    // TODO("figure out folder update key issues. I'd rather have just 1 but it seems like the side sheet doesn't close when the folder is deleted if that's the case - something to do with a rendering race condition? idk")
    val sideSheetOpacity = animateFloatAsState(targetValue = if (sideSheetStatus is NoSideSheet) 0f else 1f)
    // same as sideSheetUpdateKey, but for changes originating from FolderPage
    var sideSheetUpdateKey by remember { mutableStateOf(0) }
    var folderPageUpdateKey by remember { mutableStateOf(0) }
    var actionButtonsUpdateKey by remember { mutableStateOf(0) }
    var shouldShowHeader by remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value
    // for uploading folders
    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        appViewModel.closeModal()
        if (modalState == SelectingFolderUpload && directory?.isDirectory() ?: false && currentRoute?.destination?.route?.contains(
                FolderRoute::class.simpleName!!
            ) ?: false
        ) {
            val folderId = currentRoute.toRoute<FolderRoute>().id
            appViewModel.uploadFolder(directory, folderId)
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
                    sideSheetActive = sideSheetStatus !is NoSideSheet,
                    onCreateFolderClick = { appViewModel.openModal(CreatingEmptyFolder) },
                    onUploadFolderClick = { appViewModel.openModal(SelectingFolderUpload) }
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
                        refreshKey = sideSheetUpdateKey + actionButtonsUpdateKey + headerUpdateKey,
                        onFolderInfo = { appViewModel.sideSheetItem(it) },
                        onFileInfo = { appViewModel.sideSheetItem(it) },
                        onUpdate = { folderPageUpdateKey += 1 },
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
        }
        StandardSideSheet(
            modifier = Modifier.weight(1.01f - mainContentWidth.value, true).alpha(sideSheetOpacity.value),
            onCloseAction = appViewModel::closeSideSheet
        ) {
            when (sideSheetStatus) {
                is FolderSideSheet -> {
                    val viewModel = koinInject<FolderDetailViewModel> { parametersOf(sideSheetStatus.folder.id) }
                    FolderDetailSheet(
                        viewModel = viewModel,
                        closeSelf = { appViewModel.sideSheetItem(null) },
                        refreshKey = folderPageUpdateKey + actionButtonsUpdateKey
                    ) {
                        sideSheetUpdateKey += 1
                    }
                }

                is FileSideSheet -> TODO("FileSideSheet not implemented")
                is NoSideSheet -> {}
            }
        }
        // app modals
        when (modalState) {
            CreatingEmptyFolder -> TextDialog(
                title = "Create empty folder",
                bodyText = "Folder name",
                onCancel = { appViewModel.closeModal() },
                confirmText = "Create",
                onConfirm = {
                    if (it.isNotBlank()) {
                        appViewModel.closeModal()
                        appViewModel.addEmptyFolder(it)
                        actionButtonsUpdateKey += 1
                    }
                })
            is ApplicationErrorModal -> Dialog(
                title = "Failed to upload folder",
                onDismissRequest = { appViewModel.closeModal() },
                text = "PLACEHOLDER ERROR MESSAGE: ${modalState.message}",
                icon = Icons.Default.Error
            )

            is LoadingModal -> LoadingModalDialog(modalState)
            SelectingFolderUpload -> directoryPicker.launch()
            NoModal -> {}
        }
    }
}
