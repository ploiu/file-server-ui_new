package dev.ploiu.file_server_ui_new.service

sealed interface RetrieveCredsResult
data class RetrieveCredsError(val message: String) : RetrieveCredsResult

/** No credential is found. Safe to ignore, but also means the user should be prompted to store them */
class NoCredsFound : RetrieveCredsResult
data class RetrieveCredsSuccess(val username: String, val password: String) : RetrieveCredsResult

sealed interface SaveCredsResult
data class SaveCredsError(val message: String) : SaveCredsResult
class SaveCredsSuccess : SaveCredsResult

interface CredsService {

    /**
     * retrieves the credentials to the server for the current user
     */
    suspend fun retrieveCreds(): RetrieveCredsResult

    /**
     * saves the credentials as an encrypted value using the default OS keystore (encrypted app storage for android, rust_credential_manager for desktop)
     */
    suspend fun saveCreds(username: String, password: String): SaveCredsResult

}
