package dev.ploiu.file_server_ui_new

import dagger.Component
import dev.ploiu.file_server_ui_new.views.LoadingView
import javax.inject.Singleton

@Singleton
@Component
interface DesktopComponent {
    fun loadingView(): LoadingView
}
