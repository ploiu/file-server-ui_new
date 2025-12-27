package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import dev.ploiu.file_server_ui_new.util.processResponse
import dev.ploiu.file_server_ui_new.util.processResponseUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream

class FolderService(private val client: FolderClient) {

    private val log = KotlinLogging.logger {}

    suspend fun getFolder(id: Long): Result<FolderApi, String> {
        if (id < 0) {
            return Err("id ($id) must be >= 0")
        }
        val res = processResponse(client.getFolder(id))
        return res.mapError { it.message }
    }

    suspend fun downloadFolder(id: Long): Result<InputStream, String> {
        if (id <= 0) {
            return Err("id ($id) must be > 0")
        }
        val processed = processResponse(client.downloadFolder(id))
        return processed.map { it.byteStream() }.mapError { it.message }
    }

    @Deprecated("Use PreviewClient instead for streaming previews")
    suspend fun getPreviewsForFolder(id: Long): Result<BatchFilePreview, String> {
        if (id < 0) {
            return Err("id ($id) must be >= 0")
        }
        val res = processResponse(client.getPreviewsForFolder(id))
        return res.map { rawPreviews ->
                rawPreviews.mapValues { entry ->
                        entry.value.map { it.toByte() }.toByteArray()
                    }
            }.mapError { it.message }
    }

    suspend fun createFolder(req: CreateFolder): Result<FolderApi, String> {
        if (req.name.isBlank()) {
            return Err("name cannot be blank")
        }
        if (req.parentId < 0) {
            return Err("parentId (${req.parentId}) must be >= 0")
        }
        return processResponse(client.createFolder(req)).mapError { it.message }
    }

    suspend fun updateFolder(req: UpdateFolder): Result<FolderApi, String> {
        if (req.name.isBlank()) {
            return Err("name cannot be empty")
        }
        if (req.parentId < 0) {
            return Err("parentId (${req.parentId}) must be >= 0")
        }
        if (req.id <= 0) {
            return Err("id (${req.id}) must be > 0")
        }
        return processResponse(client.updateFolder(req)).mapError { it.message }
    }

    suspend fun deleteFolder(id: Long): Result<Unit, String> {
        if (id <= 0) {
            return Err("id ($id) must be > 0")
        }
        return processResponseUnit(client.deleteFolder(id)).mapError { it.message }
    }

    /**
     * Checks if the folder with the passed [folderId] has any direct child folder / file
     * with a case-insensitive name match inside [checkNames]
     */
    suspend fun hasNameClash(folderId: Long, checkNames: Collection<String>): Result<Boolean, String> =
        getFolder(folderId).map { folder ->
            val childNames = folder.folders.map { it.name.lowercase() } + folder.files.map { it.name.lowercase() }
            val cleanedCheckNames = checkNames.map { it.lowercase() }.toSet()
            cleanedCheckNames.intersect(childNames.toSet()).isNotEmpty()
        }
}
