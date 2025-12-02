package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.service.*
import dev.ploiu.file_server_ui_new.viewModel.*
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val pageModule = module {
    viewModelOf(::LoadingPageViewModel)
    viewModel { (folderId: Long) -> FolderPageViewModel(get(), get(), get(), folderId) }
    viewModel { (searchTerm: String) -> SearchResultsPageViewModel(get(), get(), searchTerm) }
    viewModel { (folderId: Long) -> FolderDetailViewModel(get(), folderId) }
    viewModel { (fileId: Long) -> FileDetailViewModel(get(), get(), get(), fileId) }
    viewModelOf(::ApplicationViewModel)
    viewModelOf(::LoginPageViewModel)
}

val desktopServiceModule = module {
    single<PreviewService> { DesktopPreviewService(get(), get(), get()) }
    single<DirectoryService> { DirectoryService() }
    single<FolderUploadService> { DesktopFolderUploadService(get(), get()) }
}
