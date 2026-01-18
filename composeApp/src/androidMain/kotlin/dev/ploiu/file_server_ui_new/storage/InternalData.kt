package dev.ploiu.file_server_ui_new.storage

import androidx.datastore.core.Serializer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private val log = KotlinLogging.logger("InternalDataStore")

/**
 * values that need to be encrypted before placing into the data store
 * @param [credentials] the saved username and password, if the user has elected to save them
 */
@Serializable
data class EncryptedSettings(val credentials: String?)

object EncryptedSettingsSerializer : Serializer<EncryptedSettings> {
    override val defaultValue = EncryptedSettings(null)

    override suspend fun readFrom(input: InputStream): EncryptedSettings {
        return try {
            Json.decodeFromString<EncryptedSettings>(input.readBytes().decodeToString())
        } catch (e: Exception) {
            log.error(e) { "Failed to read encrypted user settings from store" }
            defaultValue
        }
    }

    override suspend fun writeTo(
        t: EncryptedSettings,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(t).encodeToByteArray())
        }
    }

}
