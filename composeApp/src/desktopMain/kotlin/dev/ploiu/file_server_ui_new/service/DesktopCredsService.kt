package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.ffi.creds.*

private const val APP_NAME = "ploiu-file-server"

class DesktopCredsService : CredsService {
    override suspend fun saveCreds(username: String, password: String): SaveCredsResult {
        val appId = AppSettings.sandboxedAppId
        return when (CredsLib.storeCredential(APP_NAME + "_$appId", username, password)) {
            SavePasswordResult.SUCCESS -> SaveCredsSuccess()
            SavePasswordResult.OS_ERROR -> SaveCredsError("An underlying error with the OS occurred")
            SavePasswordResult.LENGTH_ERROR -> SaveCredsError("The username or password you input is too long")
            SavePasswordResult.GENERIC_ERROR -> SaveCredsError("An unknown error occurred")
        }
    }

    override suspend fun retrieveCreds(): RetrieveCredsResult {
        val appId = AppSettings.sandboxedAppId
        return when (val res = CredsLib.retrieveCredential(APP_NAME + "_$appId")) {
            is Success -> res.into()
            is NotFoundError -> NoCredsFound()
            is InvalidFormatError -> RetrieveCredsError("Credential format is corrupted! Check your OS keystore")
            is OSError -> RetrieveCredsError("An underlying error with the OS occurred")
            is GenericError -> RetrieveCredsError("An unknown error occurred retrieving your username and password")
        }
    }
}
