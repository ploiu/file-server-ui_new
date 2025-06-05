package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.service.PreviewService
import dev.ploiu.file_server_ui_new.views.FolderView
import dev.ploiu.file_server_ui_new.views.LoadingView
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val componentViewModule = module {
    viewModelOf(::LoadingView)
    viewModel { (folderId: Long) -> FolderView(get(), folderId) }
}

val desktopServiceModule = module {
    single<PreviewService> { PreviewService(get(), get()) }
}
