package dev.ploiu.file_server_ui_new

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ploiu.file_server_ui_new.module.*
import dev.ploiu.file_server_ui_new.pages.LoginPage
import dev.ploiu.file_server_ui_new.viewModel.AndroidApplicationViewModel
import dev.ploiu.file_server_ui_new.viewModel.FolderRoute
import dev.ploiu.file_server_ui_new.viewModel.LoginRoute
import dev.ploiu.file_server_ui_new.viewModel.ModalController
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.android.ext.koin.androidContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin

class App : Application() {
    private val log = KotlinLogging.logger { }
    override fun onCreate() {
        super.onCreate()
        log.info { "Starting app" }
        try {
            startKoin {
                androidContext(this@App)
                modules(pageModule, configModule, clientModule, serviceModule, miscModule, androidServiceModule)
            }
        } catch (_: Exception) {
            println("koin already started")
        }
    }
}

@Composable
fun AppContent(
    appViewModel: AndroidApplicationViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController(),
    modalController: ModalController = koinViewModel(),
) {
    appViewModel.state.collectAsState().value
    val currentRoute = navController.currentBackStackEntryAsState().value
    // for uploading files
    rememberFilePickerLauncher(mode = FileKitMode.Multiple()) { files ->
        if (files != null && currentRoute?.destination?.route?.contains(FolderRoute::class.simpleName!!) ?: false) {
            val folderId = currentRoute.toRoute<FolderRoute>().id
            appViewModel.uploadBulk(files, folderId)
        }
    }

    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            NavHost(navController = navController, startDestination = LoginRoute()) {
                composable<LoginRoute> {
                    LoginPage(navController = navController)
                }
            }
        }
    }
}
