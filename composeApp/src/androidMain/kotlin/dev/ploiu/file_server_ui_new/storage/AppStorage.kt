package dev.ploiu.file_server_ui_new.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull

object AppStorage {
    private val log = KotlinLogging.logger {}
    private val Context.datastore: DataStore<EncryptedSettings> by dataStore(
        fileName = "encryptedSettings.json",
        serializer = EncryptedSettingsSerializer,
    )

    suspend fun Context.getSavedPassword(): String? = _getSavedPassword(this)

    suspend fun Context.savePassword(creds: String?) = _savePassword(this, creds)

    private suspend fun _getSavedPassword(context: Context): String? = context.datastore.data.firstOrNull()?.credentials

    private suspend fun _savePassword(context: Context, creds: String?): Boolean {
        return try {
            context.datastore.updateData { settings -> settings.copy(credentials = creds) }
            true
        } catch (e: Exception) {
            log.error(e) { "Failed to save creds" }
            false
        }
    }
}
