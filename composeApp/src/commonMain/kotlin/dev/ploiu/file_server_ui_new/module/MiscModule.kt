package dev.ploiu.file_server_ui_new.module

import dev.ploiu.file_server_ui_new.viewModel.ModalController
import org.koin.dsl.module

val miscModule = module {
    single<ModalController> { ModalController() }
}
