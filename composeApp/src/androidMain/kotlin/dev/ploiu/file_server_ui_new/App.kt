package dev.ploiu.file_server_ui_new

import android.app.Application
import dev.ploiu.file_server_ui_new.module.clientModule
import dev.ploiu.file_server_ui_new.module.configModule
import dev.ploiu.file_server_ui_new.module.miscModule
import dev.ploiu.file_server_ui_new.module.pageModule
import dev.ploiu.file_server_ui_new.module.serviceModule
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    private val log = KotlinLogging.logger { }
    override fun onCreate() {
        super.onCreate()
        log.info { "Starting app" }
        try {
            startKoin {
                androidContext(this@App)
                modules(pageModule, configModule, clientModule, serviceModule, miscModule)
            }
        } catch (_: Exception) {
            println("koin already started")
        }
    }
}
