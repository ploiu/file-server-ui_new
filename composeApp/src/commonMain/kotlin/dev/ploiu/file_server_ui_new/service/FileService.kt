package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import dev.ploiu.file_server_ui_new.SearchParser
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.model.CreateFileRequest
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FileRequest
import dev.ploiu.file_server_ui_new.processResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.net.URLConnection

class FileService(val client: FileClient) {

    companion object {
        private val EXTENSION_PATTERN = "\\..+$".toRegex()
    }

    // TODO download file (possibly have to do different implementations)

    suspend fun getMetadata(id: Long): Result<FileApi, String> {
        if (id < 0) {
            return Err("id ($id) must be >= 0")
        }
        val res = processResponse(client.getMetadata(id))
        return res.mapError { it.message }
    }

    suspend fun search(searchText: String): Result<Collection<FileApi>, String> {
        val (text, tags, attributes) = SearchParser.parse(searchText)
        val res = processResponse(client.search(text, tags, attributes))
        return res.mapError { it.message }
    }

    suspend fun deleteFile(id: Long): Result<Unit, String> {
        if (id < 0) {
            return Err("id ($id) cannot be less than 0")
        }
        return processResponse(client.deleteFile(id)).mapError { it.message }
    }

    suspend fun updateFile(req: FileRequest): Result<FileApi, String> {
        if (req.id < 0) {
            return Err("id (${req.id}) cannot be less than 0")
        }
        if (req.name.isBlank()) {
            return Err("Name cannot be blank.")
        }
        return processResponse(client.updateFile(req)).mapError { it.message }
    }

    suspend fun createFile(req: CreateFileRequest): Result<FileApi, String> {
        val (folderId, file, force) = req
        if (!file.exists()) {
            return Err("No file at path (${file.absolutePath}) exists")
        }
        val mimeType = URLConnection.guessContentTypeFromName(file.name) ?: "text/plain"
        val (name, extension) = splitFileName(file.name)
        // TODO figure out why file names were getting ( and ) replaced with leftParenthese and rightParenthese in the client even
        //  though the server does it...also why again were we doing that?
        val filePart = MultipartBody.Part.createFormData(
            "file",
            name,
            RequestBody.create(file = file, contentType = mimeType.toMediaTypeOrNull())
        )
        val folderPart = MultipartBody.Part.createFormData("folderId", folderId.toString())
        val extensionPart = if (extension != null) MultipartBody.Part.createFormData("extension", extension) else null
        return processResponse(client.createFile(filePart, extensionPart, folderPart)).mapError { it.message }
    }

    /**
     * splits the passed name into up to 2 parts, file name will always be index 0, and file extension (if it exists) will always be index 1
     * This is meant to give more accurate detection of file name extensions due to how the server behaves (it uses rocket)
     *
     * @param name
     * @return
     */
    fun splitFileName(name: String): SplitName {
        if (name.startsWith(".")) {
            return SplitName("", name.substring(1))
        }
        val match = EXTENSION_PATTERN.find(name)
        return if (match != null) {
            val extIndex = match.range.first
            SplitName(name.substring(0, extIndex), name.substring(extIndex + 1))
        } else {
            SplitName(name, null)
        }
    }

}

data class SplitName(val name: String, val extension: String?)
