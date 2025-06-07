package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.BadRequestException
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import dev.ploiu.file_server_ui_new.processResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.ResponseBody

class FolderService(val client: FolderClient) {

    private val log = KotlinLogging.logger {}

    suspend fun getFolder(id: Long): FolderApi {
        if(id < 0) {
            throw BadRequestException("id ($id) must be >= 0")
        }
        return processResponse(client.getFolder(id))
    }

    suspend fun downloadFolder(id: Long): ResponseBody {
        if (id <= 0) {
            throw BadRequestException("id ($id) must be > 0")
        }
        return processResponse(client.downloadFolder(id))
    }

    suspend fun getPreviewsForFolder(id: Long): Map<Long, ByteArray> {
        if (id < 0) {
            throw BadRequestException("id ($id) must be >= 0")
        }
        val rawPreviews = processResponse(client.getPreviewsForFolder(id))
        return rawPreviews.mapValues { entry -> entry.value.map { it.toByte() }.toByteArray() }
    }

    suspend fun createFolder(req: CreateFolder): FolderApi {
        if (req.name.isBlank()) {
            throw BadRequestException("name cannot be blank")
        }
        if (req.parentId < 0) {
            throw BadRequestException("parentId (${req.parentId}) must be >= 0")
        }
        return processResponse(client.createFolder(req))
    }

    suspend fun updateFolder(req: UpdateFolder): FolderApi {
        if (req.name.isBlank()) {
            throw BadRequestException("name cannot be empty")
        }
        if (req.parentId < 0) {
            throw BadRequestException("parentId (${req.parentId}) must be >= 0")
        }
        if (req.id <= 0) {
            throw BadRequestException("id (${req.id}) must be > 0")
        }
        return processResponse(client.updateFolder(req))
    }

    suspend fun deleteFolder(id: Long) {
        if (id <= 0) {
            throw BadRequestException("id ($id) must be > 0")
        }
        processResponse(client.deleteFolder(id))
    }
}
