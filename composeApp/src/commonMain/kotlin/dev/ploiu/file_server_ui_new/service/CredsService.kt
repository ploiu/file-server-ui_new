package dev.ploiu.file_server_ui_new.service

sealed interface RetrieveCredsResult
data class RetrieveCredsError(val message: String) : RetrieveCredsResult

/** No credential is found. Safe to ignore, but also means the user should be prompted to store them */
class NoCredsFound : RetrieveCredsResult
data class RetrieveCredsSuccess(val username: String, val password: String) : RetrieveCredsResult

sealed interface SaveCredsResult
data class SaveCredsError(val message: String) : SaveCredsResult
class SaveCredsSuccess : SaveCredsResult

/**
 * retrieves the credentials to the server for the current user
 */
expect fun retrieveCreds(): RetrieveCredsResult

/**
 * saves the credentials as an encrypted value using the default OS keystore (encrypted app storage for android, rust_credential_manager for desktop)
 */
expect fun saveCreds(username: String, password: String): SaveCredsResult
