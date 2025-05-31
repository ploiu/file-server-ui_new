package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.client.ApiClient
import dev.ploiu.file_server_ui_new.config.ServerConfig
import dev.ploiu.file_server_ui_new.model.Metadata
import jakarta.inject.Inject

class ApiService(@Inject val serverConfig: ServerConfig, @Inject val client: ApiClient) {
    suspend fun getApiInfo() = client.getApiInfo()
}
