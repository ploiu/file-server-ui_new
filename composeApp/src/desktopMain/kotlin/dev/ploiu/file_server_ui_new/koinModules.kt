package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.service.*
import dev.ploiu.file_server_ui_new.viewModel.*
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val pageModule = module {
    viewModelOf(::LoadingPageViewModel)
    viewModel { (folderId: Long) -> FolderPageViewModel(get(), get(), get(), folderId, get()) }
    viewModel { (searchTerm: String) -> SearchResultsPageViewModel(get(), get(), searchTerm, get()) }
    viewModel { (folderId: Long) -> FolderDetailViewModel(get(), folderId, get()) }
    viewModel { (fileId: Long) -> FileDetailViewModel(get(), get(), get(), fileId, get()) }
    viewModelOf(::DesktopApplicationViewModel)
    viewModelOf(::LoginPageViewModel)
}

val desktopServiceModule = module {
    single<PreviewService> { DesktopPreviewService(get(), get()) }
    single<FolderUploadService> { DesktopFolderUploadService(get(), get()) }
}
