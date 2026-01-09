package dev.ploiu.file_server_ui_new.service

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

class AndroidFolderUploadService(private val folderService: FolderService, private val fileService: FileService) :
    FolderUploadService {
    override suspend fun uploadFolder(
        folder: PlatformFile,
        parentFolderId: Long,
    ): Flow<BatchUploadResult> {
        TODO("Not yet implemented")
    }

}
