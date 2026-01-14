package dev.ploiu.file_server_ui_new

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.ploiu.file_server_ui_new.MessageTypes.*
import dev.ploiu.file_server_ui_new.components.*
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
import dev.ploiu.file_server_ui_new.service.AppSettings
import dev.ploiu.file_server_ui_new.ui.theme.darkScheme
import dev.ploiu.file_server_ui_new.ui.theme.lightScheme
import dev.ploiu.file_server_ui_new.viewModel.*
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.appIcon
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.isDirectory
import org.jetbrains.compose.resources.painterResource
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

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    try {
        startKoin {
            modules(configModule, clientModule, serviceModule, pageModule, desktopServiceModule, miscModule)
        }
    } catch (_: Exception) {
        println("Koin already started")
    }
    // pre-cache all generic file icons
    loadResources()
    FileKit.init(
        appId = "PloiuFileServer",
        filesDir = AppSettings.getRootDirectory().file,
        cacheDir = AppSettings.getCacheDir().file,
    )
    val messagePasser = remember { ObservableMessagePasser() }
    // TODO window breakpoints (jetbrains has a lib, see https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-adaptive-layouts.html)
    Window(
        onCloseRequest = ::exitApplication,
        title = "file-server-ui_new",
        state = rememberWindowState(width = 1200.dp, height = 600.dp),
        icon = painterResource(Res.drawable.appIcon),
        onKeyEvent = {
            if (it.type == KeyEventType.KeyUp) {
                when (it.key) {
                    Key.K -> {
                        if (it.isCtrlPressed) {
                            messagePasser.passMessage(FOCUS_SEARCHBAR)
                        }
                    }

                    Key.Escape -> messagePasser.passMessage(HIDE_ACTIVE_ELEMENT)
                    Key.MoveHome -> messagePasser.passMessage(JUMP_TO_TOP)
                    Key.MoveEnd -> messagePasser.passMessage(JUMP_TO_BOTTOM)
                    Key.DirectionLeft -> {
                        if (it.isAltPressed) {
                            messagePasser.passMessage(NAVIGATE_BACKWARDS)
                        }
                    }

                    else -> {
                    }
                }
            }
            true
        },
    ) {
        AppTheme {
            MainDesktopBody(
                appViewModel = koinInject(),
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // checking buttons.isBackPressed does not work on my mouse (key codes listed at https://github.com/l5l/Table-of-vk-and-sc-codes-keyboard-keys-and-mouse-buttons#XButton1)
                            if (event.buttons.isBackPressed || event.type == PointerEventType.Press && event.button!! == PointerButton(
                                    index = 5,
                                )
                            ) {
                                messagePasser.passMessage(NAVIGATE_BACKWARDS)
                            }
                        }
                    }
                },
                messagePasser = messagePasser,
                window = this.window,
            )
        }
    }
}

// some routes shouldn't have the app header show up
val headerlessRoutes = listOf(
    LoginRoute::class.qualifiedName,
    LoadingRoute::class.qualifiedName,
)

private fun isHeaderless(route: String?) = headerlessRoutes.contains(route)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainDesktopBody(
    appViewModel: DesktopApplicationViewModel,
    messagePasser: ObservableMessagePasser,
    window: ComposeWindow,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    modalController: ModalController = koinViewModel(),
) {
    // not managed within navBar because navigation external to that component (like clicking a folder) gets bubbled up here
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
    val mousePosition = rememberMousePos(window)
    val appState = appViewModel.state.collectAsState().value
    val searchBarFocuser = remember { FocusRequester() }
    val mainContentWidth = animateFloatAsState(
        targetValue = if (appState.sideSheetState is NoSideSheet) 1f else .7f,
        animationSpec = tween(),
    )
    val sideSheetOpacity = animateFloatAsState(targetValue = if (appState.sideSheetState is NoSideSheet) 0f else 1f)
    // same as sideSheetUpdateKey, but for changes originating from FolderPage
    var shouldShowHeader by remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value
    // for uploading folders
    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        // the second part of this check is to make sure we can't upload a folder unless we are on a folder page, since we perform a cast later on
        if (directory?.isDirectory() ?: false && currentRoute?.destination?.route?.contains(
                FolderRoute::class.simpleName!!,
            ) ?: false
        ) {
            val folderId = currentRoute.toRoute<FolderRoute>().id
            appViewModel.uploadBulk(listOf(directory), folderId)
        }
    }
    // for uploading files
    val filePicker = rememberFilePickerLauncher(mode = FileKitMode.Multiple()) { files ->
        if (files != null && currentRoute?.destination?.route?.contains(FolderRoute::class.simpleName!!) ?: false) {
            val folderId = currentRoute.toRoute<FolderRoute>().id
            appViewModel.uploadBulk(files, folderId)
        }
    }
    // used to determine if the header should be replaced with a box that lets the user upload dragged and dropped files/folders
    // is any drag-and-drop active
    var dragStatus by remember { mutableStateOf<DragStatus>(NotDragging) }
    // used to keep track of the current app header height, so that the upload files box doesn't cause a layout shift.
    // logic is that if you're dragging and dropping, you can't resize the window (and therefore don't have to worry about sizing this based on the box)
    var searchBarHeight by remember { mutableStateOf(0.dp) }

    /** This is only used to detect if drag and drop is active */
    val detectDragAndDrop = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragStatus = NotDragging
                return false
            }

            override fun onStarted(event: DragAndDropEvent) {
                dragStatus = IsDragging(isFilesystem = event.dragData() is DragData.FilesList)
            }

            override fun onEnded(event: DragAndDropEvent) {
                dragStatus = NotDragging
            }
        }
    }
    // set up global keybinds
    DisposableEffect(Unit) {
        messagePasser handles FOCUS_SEARCHBAR { searchBarFocuser.requestFocus() }
        messagePasser handles HIDE_ACTIVE_ELEMENT {
            // we're using this instead of the state variable because at this point, the state hasn't updated yet and we need a live version
            if (modalController.isOpen) {
                // there's currently a bug in the desktop implementation of compose where modals don't close on the
                // escape key. Most are handled manually but the AlertDialog does not work even with that manual
                // handling. This is a fallback.
                modalController.close(appViewModel, true)
            } else if (!modalController.isJustClosed) {
                appViewModel.closeSideSheet()
            } else {
                // don't close anything, but be sure to reset the dialog just closed flag
                modalController.clearJustClosed()
            }
        }
        messagePasser handles NAVIGATE_BACKWARDS {
            if (navBarState.size > 1) {
                navBarState = navBarState.pop()
                navController.popBackStack()
            }
        }

        onDispose {
            messagePasser ignores FOCUS_SEARCHBAR
            messagePasser ignores HIDE_ACTIVE_ELEMENT
            messagePasser ignores NAVIGATE_BACKWARDS
        }
    }

    // show the header if the route should show one
    LaunchedEffect(currentRoute) {
        shouldShowHeader = !isHeaderless(currentRoute?.destination?.route)
    }

    Row(
        modifier = Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = detectDragAndDrop,
        ) then modifier,
    ) {
        Column(modifier = Modifier.animateContentSize().weight(mainContentWidth.value, true)) {
            if (shouldShowHeader) {
                val status = dragStatus
                // box that shows up to let you drop files from the file system
                UploadFileBox(
                    height = searchBarHeight + 8.dp,
                    shouldShow = status is IsDragging && status.isFilesystem,
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                ) {
                    if (currentRoute?.destination?.route?.contains(FolderRoute::class.simpleName!!) ?: false) {
                        val folderId = currentRoute.toRoute<FolderRoute>().id
                        appViewModel.uploadBulk(it, folderId)
                    }
                }
                if (status is NotDragging || (status is IsDragging && !status.isFilesystem)) {
                    AppHeader(
                        searchBarFocuser = searchBarFocuser,
                        navController = navController,
                        sideSheetActive = appState.sideSheetState !is NoSideSheet,
                        onCreateFolderClick = {
                            if (currentRoute?.destination?.route?.contains(FolderRoute::class.simpleName!!) ?: false) {
                                val folderId = currentRoute.toRoute<FolderRoute>().id
                                appViewModel.openCreateEmptyFolderModal(folderId)
                            }
                        },
                        onUploadFolderClick = directoryPicker::launch,
                        onUploadFileClick = filePicker::launch,
                        onHeightChange = { searchBarHeight = it },
                    )
                }
                Spacer(Modifier.height(8.dp))
                NavBar(
                    state = navBarState,
                    onFolderChildDropped = { newParent, movedChild ->
                        appViewModel.moveChildToFolder(
                            newParent,
                            movedChild,
                        )
                    },
                    clickEntry = { folders ->
                        navBarState =
                            navBarState.copy(folders = folders); navController.navigate(FolderRoute(folders.last().id))
                    },
                )
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
                        viewModel = viewModel,
                        refreshKey = appState.updateKey,
                        isDragging = dragStatus is IsDragging,
                        mousePosition = mousePosition.value,
                        messagePasser = messagePasser,
                        onFolderInfo = { appViewModel.sideSheetItem(it) },
                        onFileInfo = { appViewModel.sideSheetItem(it) },
                        onUpdate = appViewModel::changeUpdateKey,
                        onFileSystemDropped = { files, folderId -> appViewModel.uploadBulk(files, folderId) },
                        onFolderNav = {
                            navController.navigate(FolderRoute(it.id))
                            navBarState += it
                        },
                    )
                }
                composable<SearchResultsRoute> { backStack ->
                    val route: SearchResultsRoute = backStack.toRoute()
                    val viewModel = koinInject<SearchResultsPageViewModel> { parametersOf(route.searchTerm) }
                    SearchResultsPage(
                        viewModel = viewModel,
                        onUpdate = appViewModel::changeUpdateKey,
                        onFileClick = { appViewModel.sideSheetItem(it) },
                        refreshKey = appState.updateKey,
                        isDragging = dragStatus is IsDragging,
                    )
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
