package dev.ploiu.file_server_ui_new.service

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toFile
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.io.path.*

@Serializable
private data class SettingsObject(val sandboxedAppId: String)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/** General settings for how the desktop app should behave  */
object AppSettings {

    /** will be `true` if the app is being run in a debug environment and not a final build */
    val isLocalDebug = System.getenv("INTELLIJ_RUN") == "true"

    private var settings: SettingsObject

    /** the sandboxed app id used for making it harder to find creds for the app programmatically and to let both the local and prod creds exist at the same time */
    val sandboxedAppId: String
        get() = settings.sandboxedAppId

    private val _rootDirectory = (System.getProperty("user.home") + "/.ploiu-file-server" + if (isLocalDebug) {
        "_local"
    } else {
        ""
    }).toPath()

    fun getRootDirectory() = _rootDirectory.toPlatformFile()

    fun getCacheDir() = (_rootDirectory / "cache").toPlatformFile()

    init {
        val f = _rootDirectory.toFile()
        if (!f.exists()) {
            f.mkdirs()
        }
        // we can't use the sandboxed id for this because we don't know it yet, duh
        runBlocking {
            val root = getRootDirectory().file
            val settingsFile = root.toPath() / "settings.json"
            settings = if (!settingsFile.exists()) {
                settingsFile.createFile()
                val settingsObj = SettingsObject(sandboxedAppId = UUID.randomUUID().toString())
                settingsFile.writeText(json.encodeToString(settingsObj), charset = StandardCharsets.UTF_8)
                settingsObj
            } else {
                json.decodeFromString(settingsFile.readText(StandardCharsets.UTF_8))
            }
        }
    }

}

fun Path.toPlatformFile() = PlatformFile(this)
