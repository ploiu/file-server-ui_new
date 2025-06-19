package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.ApiClient
import dev.ploiu.file_server_ui_new.config.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging


/**
 * rust has broken my brain. I crave the powerful and flexible enums that rust provides. This sealed class is a
 * garbage way of emulating that ;-;
 */
sealed class ServerCompatibilityResult
class CompatibleResult : ServerCompatibilityResult()
class IncompatibleResult(val serverVersion: String, val compatibleVersion: String) : ServerCompatibilityResult()
data class ErrorResult(val error: Exception) : ServerCompatibilityResult()


class ApiService(val serverConfig: ServerConfig, val client: ApiClient) {
    private val log = KotlinLogging.logger { }

    suspend fun getApiInfo() = client.getApiInfo()

    suspend fun isCompatibleWithServer(): ServerCompatibilityResult {
        log.info("Checking if server is compatible with client (looking for pattern ${serverConfig.compatibleVersion})")
        // 🦎
        val gex = serverConfig.generateCompatibleVersionPattern()
        val serverVersion = try {
            getApiInfo().version
        } catch (e: Exception) {
            log.error(e) { "Failed to check server version" }
            return ErrorResult(e)
        }
        log.info("server version is $serverVersion")
        val matches = gex.matcher(serverVersion).find()
        return if (matches) {
            log.info("We're compatible!")
            CompatibleResult()
        } else {
            log.error("Incompatible with current server version.")
            IncompatibleResult(serverVersion, serverConfig.compatibleVersion)
        }
    }
}
