package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.ApiClient
import dev.ploiu.file_server_ui_new.config.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging

class ApiService constructor(val serverConfig: ServerConfig, val client: ApiClient) {
    private val log = KotlinLogging.logger {  }

    suspend fun getApiInfo() = client.getApiInfo()

    suspend fun isCompatibleWithServer(): Boolean {
        log.info("Checking if server is compatible with client (looking for pattern ${serverConfig.compatibleVersion}")
        val gex = serverConfig.generateCompatibleVersionPattern()
        val serverVersion = getApiInfo().version
        log.info("server version is $serverVersion")
        val matches = gex.matcher(serverVersion).find()
        if(matches) {
            log.info("We're compatible!")
        } else {
            log.error("Incompatible with current server version.")
        }
        return matches
    }
}
