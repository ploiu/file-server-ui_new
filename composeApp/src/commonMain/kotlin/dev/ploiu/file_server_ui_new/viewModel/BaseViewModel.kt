package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.ext.getFullName

open class BaseViewModel : ViewModel() {
    protected var log: KLogger = KotlinLogging.logger(name = this::class.getFullName())
}
