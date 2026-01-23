package dev.ploiu.file_server_ui_new.module

import dev.ploiu.file_server_ui_new.service.AndroidCredsService
import dev.ploiu.file_server_ui_new.service.AndroidPreviewService
import dev.ploiu.file_server_ui_new.service.CredsService
import dev.ploiu.file_server_ui_new.service.PreviewService
import dev.ploiu.file_server_ui_new.viewModel.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val pageModule = module {
    viewModelOf(::LoadingPageViewModel)
    viewModel { (folderId: Long) -> FolderPageViewModel(get(), get(), get(), folderId, get()) }
    viewModel { (searchTerm: String) -> SearchResultsPageViewModel(get(), get(), searchTerm, get()) }
    // viewModel { (folderId: Long) -> FolderDetailViewModel(get(), folderId, get()) }
    // viewModel { (fileId: Long) -> FileDetailViewModel(get(), get(), get(), fileId, get()) }
    viewModelOf(::AndroidApplicationViewModel)
    viewModelOf(::LoginPageViewModel)
}

val androidServiceModule = module {
    single<CredsService> { AndroidCredsService(androidContext()) }
    single<PreviewService> { AndroidPreviewService(get(), get(), androidContext()) }
}
