package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

sealed class BatchUploadResult(val errorMessage: String?) {
    val isSuccess: Boolean
        get() = errorMessage == null
}

class BatchUploadFolderResult(val folder: FolderApi?, errorMessage: String? = null) : BatchUploadResult(errorMessage)

class BatchUploadFileResult(val file: FileApi?, errorMessage: String? = null) : BatchUploadResult(errorMessage)

interface FolderUploadService {
    /**
     * uploads a folder and all folders and files contained within. This will return Ok(Unit) if all uploads succeeded.
     * If any file / folder uploads failed, it will be included as an error message in the Err
     */
    suspend fun uploadFolder(folder: PlatformFile, parentFolderId: Long): Flow<BatchUploadResult>
}
