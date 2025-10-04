package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.*
import dev.ploiu.file_server_ui_new.model.CreateFileRequest
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApproximator
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File


class DesktopFolderUploadService(private val folderService: FolderService, private val fileService: FileService) :
    FolderUploadService {
    override suspend fun uploadFolder(
        folder: PlatformFile, parentFolderId: Long
    ): Result<Unit, Collection<String>> = internalUploadFolder(folder.file, parentFolderId)

    suspend fun internalUploadFolder(folder: File, parentFolderId: Long): Result<Unit, Collection<String>> {
        // arbitrary number and size of files being uploaded, so care must be taken here to not destroy the poor raspberry pi the server is running on.
        // also, we need to make sure the folder doesn't already exist. folder might not be the current folder and therefore won't have populated data
        val res = folderService.getFolder(parentFolderId).flatMap { parent ->
            val potentialSiblingFolders = parent.folders.map { it.name }
            if (folder.name in potentialSiblingFolders) {
                Err("A folder with the name ${folder.name} already exists in this folder")
            } else { // no folder exists with that name, so let's start
                folderService.createFolder(CreateFolder(name = folder.name, parentId = parent.id, tags = listOf()))
            }
        }
        if (res.isErr) {
            return Err(listOf(res.unwrapError()))
        } else {
            val created = res.unwrap()
            // folder has been created, but to prevent the server from being overloaded, we need to "window" the files in groups of a good number (like 30), upload them all, and then wait for them to finish uploading
            val approximation = FolderApproximator.convertDir(folder, 1)
            val chunkedFiles = approximation.childFiles.chunked(30)
            val uploadErrors = mutableListOf<String>()
            for (chunk in chunkedFiles) {
                uploadBatch(chunk, created.id)
                    .onFailure { uploadErrors.addAll(it) }
            }
            return if (approximation.childFolders.isEmpty()) {
                if (uploadErrors.isEmpty()) {
                    Ok(Unit)
                } else {
                    Err(uploadErrors)
                }
            } else {
                for (childFolder in approximation.childFolders) {
                    internalUploadFolder(childFolder.self, created.id)
                        .onFailure { errors -> uploadErrors.addAll(errors) }
                }
                if (uploadErrors.isEmpty()) {
                    Ok(Unit)
                } else {
                    Err(uploadErrors)
                }
            }
        }
    }

    /**
     * uploads a batch of files to the specified folder.
     *
     */
    private suspend fun uploadBatch(files: List<File>, folderId: Long): Result<Unit, Collection<String>> =
        coroutineScope {
            // I feel like this is too clever. It's just uploading all the files and keeping track of the ones that failed
            val errors =
                files.map { file ->
                    async {
                        fileService.createFile(CreateFileRequest(file = file, folderId = folderId, force = false))
                            .mapError { e -> "Failed to upload file ${file.path}: $e" }
                    }
                }.awaitAll()
                    .mapNotNull { it.getError() }
                    .toList()
            if (errors.isEmpty()) {
                Ok(Unit)
            } else {
                Err(errors)
            }
        }
}
