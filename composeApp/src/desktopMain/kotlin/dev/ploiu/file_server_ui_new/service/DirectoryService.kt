package dev.ploiu.file_server_ui_new.service

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.io.files.Path

/** exists solely to allow mocking directories during tests  */
object DirectoryService {
    private val _rootDirectory = (System.getProperty("user.home") + "/.ploiu-file-server").toPath()

    fun getRootDirectory() = _rootDirectory.toPlatformFile()

    fun getCacheDir() = (_rootDirectory / "cache").toPlatformFile()

}

fun Path.toPlatformFile() = PlatformFile(this)
