package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.processResponse

class FileService(val client: FileClient) {
    // TODO methods and put into module

    suspend fun getMetadata(id: Long): Result<FileApi, String> {
        if(id < 0) {
            return Err("id ($id) must be >= 0")
        }
        val res = processResponse(client.getMetadata(id))
        return res.mapError { it.message }
    }

    suspend fun search(inputText: String): Result<Collection<FileApi>, String> {
        TODO()
    }
}
