package dev.ploiu.file_server_ui_new.service

import android.content.Context
import dev.ploiu.file_server_ui_new.storage.AppStorage

private const val SEPARATOR = "\u001E"

class AndroidCredsService(private val context: Context) : CredsService {
    override suspend fun retrieveCreds(): RetrieveCredsResult {
        val creds = with(context) {
            AppStorage.run { getSavedPassword() }
        }

        return if (creds == null) {
            NoCredsFound()
        } else {
            // TODO decrypt!
            val (username, password) = splitPassword(creds)
            RetrieveCredsSuccess(username = username, password = password)
        }
    }

    override suspend fun saveCreds(
        username: String,
        password: String,
    ): SaveCredsResult {
        // TODO encrypt!
        val res = with(context) {
            AppStorage.run { savePassword(separatePassword(username, password)) }
        }
        return if (res) {
            SaveCredsSuccess()
        } else {
            SaveCredsError("Failed to save credentials")
        }
    }

    // uses the same separator as creds-ffi
    private fun separatePassword(username: String, password: String) = "$username$SEPARATOR$password"

    private fun splitPassword(creds: String) = creds.split(SEPARATOR)

}
