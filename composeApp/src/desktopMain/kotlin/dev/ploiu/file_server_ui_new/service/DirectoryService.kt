package dev.ploiu.file_server_ui_new.service

import java.io.File

/** exists solely to allow mocking directories during tests  */
class DirectoryService {
    fun getRootDirectory() = File(System.getProperty("user.home") + "/.ploiu-file-server")
}
