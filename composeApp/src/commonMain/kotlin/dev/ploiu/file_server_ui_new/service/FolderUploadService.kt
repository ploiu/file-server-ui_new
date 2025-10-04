package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Result
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.github.vinceglb.filekit.PlatformFile

interface FolderUploadService {
    suspend fun uploadFolder(folder: PlatformFile, parentFolder: FolderApi): Result<Unit, String>
}
