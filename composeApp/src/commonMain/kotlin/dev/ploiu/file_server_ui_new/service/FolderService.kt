package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.BadRequestException
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.FolderApi

class FolderService(val client: FolderClient) {
    suspend fun getFolder(id: Long): FolderApi {
        if(id < 0) {
            throw BadRequestException("id ($id) must be >= 0")
        }
        // TODO error handling
        return client.getFolder(id)
    }
}
