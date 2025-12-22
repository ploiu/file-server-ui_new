package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.*
import dev.ploiu.file_server_ui_new.model.CreateFileRequest
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApproximator
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File


class DesktopFolderUploadService(private val folderService: FolderService, private val fileService: FileService) :
    FolderUploadService {
    override suspend fun uploadFolder(
        folder: PlatformFile, parentFolderId: Long,
    ): Flow<BatchUploadResult> = internalUploadFolder(folder.file, parentFolderId)

    private fun internalUploadFolder(folder: File, parentFolderId: Long): Flow<BatchUploadResult> = flow {
        // arbitrary number and size of files being uploaded, so care must be taken here to not destroy the poor raspberry pi the server is running on.
        // also, we need to make sure the folder doesn't already exist. folder might not be the current folder and therefore won't have populated data
        val res = folderService.getFolder(parentFolderId).flatMap { parent ->
            val potentialSiblingFolders = parent.folders.map { it.name }
            if (folder.name in potentialSiblingFolders) {
                Err("A folder with the name ${folder.name} already exists in this folder")
            } else {
                // no folder exists with that name, so let's start
                folderService.createFolder(CreateFolder(name = folder.name, parentId = parent.id, tags = listOf()))
            }
        }
        if (res.isErr) {
            emit(BatchUploadFolderResult(null, res.getError()))
            return@flow
        } else {
            val created = res.unwrap()
            // folder has been created, but to prevent the server from being overloaded, we need to "window" the files in groups of a good number (like 30), upload them all, and then wait for them to finish uploading
            val approximation = FolderApproximator.convertDir(folder, 1)
            val chunkedFiles = approximation.childFiles.chunked(30)
            for (chunk in chunkedFiles) {
                emitAll(uploadBatch(chunk, created.id))
            }
            for (childFolder in approximation.childFolders) {
                val childFolderResult: Flow<BatchUploadResult> = internalUploadFolder(childFolder.self, created.id)
                emitAll(childFolderResult)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * uploads a batch of files to the specified folder.
     *
     */
    private fun uploadBatch(files: List<File>, folderId: Long): Flow<BatchUploadFileResult> = channelFlow {
        // report as each file uploads, but wait for all files to upload before returning. This prevents us from ruining the server with too much spam
        files.map { file ->
            launch(Dispatchers.IO) {
                val res = fileService.createFile(CreateFileRequest(file = file, folderId = folderId, force = false))
                val result = if (res.isOk) {
                    BatchUploadFileResult(res.unwrap(), null)
                } else {
                    BatchUploadFileResult(null, res.unwrapError())
                }
                send(result)
            }
        }.joinAll()
    }
}
