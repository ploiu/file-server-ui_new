package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Result
import io.github.vinceglb.filekit.PlatformFile

interface FolderUploadService {
    /**
     * uploads a folder and all folders and files contained within. This will return Ok(Unit) if all uploads succeeded.
     * If any file / folder uploads failed, it will be included as an error message in the Err
     */
    suspend fun uploadFolder(folder: PlatformFile, parentFolderId: Long): Result<Unit, Collection<String>>
}
