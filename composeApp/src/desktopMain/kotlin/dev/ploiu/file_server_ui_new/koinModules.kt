package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.service.DesktopPreviewService
import dev.ploiu.file_server_ui_new.service.DirectoryService
import dev.ploiu.file_server_ui_new.service.PreviewService
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailViewModel
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.LoadingPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsPageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val pageModule = module {
    viewModelOf(::LoadingPageViewModel)
    viewModel { (folderId: Long) -> FolderPageViewModel(get(), get(), folderId) }
    viewModel { (searchTerm: String) -> SearchResultsPageViewModel(get(), get(), searchTerm) }
    viewModel { (folderId: Long) -> FolderDetailViewModel(get(), folderId) }
}

val desktopServiceModule = module {
    single<PreviewService> { DesktopPreviewService(get(), get(), get()) }
    single<DirectoryService> { DirectoryService() }
}
