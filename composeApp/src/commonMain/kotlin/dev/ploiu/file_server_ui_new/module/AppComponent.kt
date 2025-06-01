package dev.ploiu.file_server_ui_new.module

import dagger.Component
import dev.ploiu.file_server_ui_new.ApiService
import jakarta.inject.Singleton

@Singleton
@Component(modules = [ApiModule::class, ConfigModule::class])
interface AppComponent {
    fun apiService(): ApiService
}
