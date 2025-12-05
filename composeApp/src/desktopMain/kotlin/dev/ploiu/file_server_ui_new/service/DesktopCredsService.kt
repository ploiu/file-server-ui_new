package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.ffi.creds.*

// TODO pull name from config file so that I can have separate development creds and production creds
private const val APP_NAME = "ploiu-file-server"

/** TODO app config file - unique app uuid to make finding creds harder programmatically */
actual fun saveCreds(username: String, password: String): SaveCredsResult {
    return when (CredsLib.storeCredential(APP_NAME, username, password)) {
        SavePasswordResult.SUCCESS -> SaveCredsSuccess()
        SavePasswordResult.OS_ERROR -> SaveCredsError("An underlying error with the OS occurred")
        SavePasswordResult.LENGTH_ERROR -> SaveCredsError("The username or password you input is too long")
        SavePasswordResult.GENERIC_ERROR -> SaveCredsError("An unknown error occurred")
    }
}

actual fun retrieveCreds(): RetrieveCredsResult {
    return when (val res = CredsLib.retrieveCredential(APP_NAME)) {
        is Success -> res.into()
        is NotFoundError -> NoCredsFound()
        is InvalidFormatError -> RetrieveCredsError("Credential format is corrupted! Check your OS keystore")
        is OSError -> RetrieveCredsError("An underlying error with the OS occurred")
        is GenericError -> RetrieveCredsError("An unknown error occurred retrieving your username and password")
    }
}
