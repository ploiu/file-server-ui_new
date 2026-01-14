package dev.ploiu.file_server_ui_new.module

import dev.ploiu.file_server_ui_new.viewModel.AndroidApplicationViewModel
import dev.ploiu.file_server_ui_new.viewModel.ApplicationViewModel
import dev.ploiu.file_server_ui_new.viewModel.FolderPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.LoadingPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.LoginPageViewModel
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsPageViewModel
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
