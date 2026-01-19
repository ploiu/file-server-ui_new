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
 * values that need are used to configure the application itself
 * @param [credentials] the saved username and password, if the user has elected to save them. Must be encrypted before writing
 */
@Serializable
data class SettingsObject(val credentials: ByteArray?, val appId: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SettingsObject

        if (!credentials.contentEquals(other.credentials)) return false
        if (appId != other.appId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = credentials?.contentHashCode() ?: 0
        result = 31 * result + (appId?.hashCode() ?: 0)
        return result
    }
}

object SettingsObjectSerializer : Serializer<SettingsObject> {
    override val defaultValue = SettingsObject(credentials = null, appId = null)

    override suspend fun readFrom(input: InputStream): SettingsObject {
        return try {
            Json.decodeFromString<SettingsObject>(input.readBytes().decodeToString())
        } catch (e: Exception) {
            log.error(e) { "Failed to read application settings from store" }
            defaultValue
        }
    }

    override suspend fun writeTo(
        t: SettingsObject,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(t).encodeToByteArray())
        }
    }

}
