package dev.ploiu.file_server_ui_new.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object AppSettings {
    private val log = KotlinLogging.logger {}

    private lateinit var appContext: Context

    /**
     * this *MUST* be called on app boot or else AppSettings will not work
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalUnsignedTypes::class)
    val appId: String by lazy {
        with(appContext) {
            runBlocking {
                val settings = appSettingsStore.data.first()
                val id = settings.appId ?: Uuid.random().toHexDashString()
                appSettingsStore.updateData { it.copy(appId = id) }
                id
            }
        }
    }

    private val Context.appSettingsStore: DataStore<SettingsObject> by dataStore(
        fileName = "appSettings.json",
        serializer = SettingsObjectSerializer,
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun getSavedPassword(): ByteArray? = with(appContext) { this.appSettingsStore.data.first().credentials }

    suspend fun savePassword(creds: ByteArray) = with(appContext) {
        try {
            this.appSettingsStore.updateData { settings -> settings.copy(credentials = creds) }
            true
        } catch (e: Exception) {
            log.error(e) { "Failed to save creds" }
            false
        }
    }
}
