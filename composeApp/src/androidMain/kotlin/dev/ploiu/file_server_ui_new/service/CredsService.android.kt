package dev.ploiu.file_server_ui_new.service

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dev.ploiu.file_server_ui_new.storage.AppSettings
import io.github.oshai.kotlinlogging.KotlinLogging

private const val SEPARATOR = "\u001E"

class AndroidCredsService(private val context: Context) : CredsService {
    private val handle: KeysetHandle
    private val log = KotlinLogging.logger {}

    init {
        val appId = AppSettings.appId
        // set up aead with tink runtime
        AeadConfig.register()
        handle = AndroidKeysetManager
            .Builder()
            .withSharedPref(this.context, appId, "key")
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://$appId")
            .build().keysetHandle
    }

    override suspend fun retrieveCreds(): RetrieveCredsResult {
        val creds = AppSettings.getSavedPassword()

        return if (creds == null) {
            NoCredsFound()
        } else {
            try {
                // TODO show biometric prompt first
                val aead = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
                val decrypted = aead.decrypt(creds, null)
                val (username, password) = splitPassword(decrypted.decodeToString())
                RetrieveCredsSuccess(username = username, password = password)
            } catch (e: Exception) {
                log.error(e) { "Failed to decrypt stored creds" }
                RetrieveCredsError("Failed to read creds")
            }
        }
    }

    override suspend fun saveCreds(
        username: String,
        password: String,
    ): SaveCredsResult {
        // TODO show biometric prompt first
        val aead = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val encrypted = aead.encrypt(separatePassword(username, password).encodeToByteArray(), null)
        val res = AppSettings.savePassword(encrypted)
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
