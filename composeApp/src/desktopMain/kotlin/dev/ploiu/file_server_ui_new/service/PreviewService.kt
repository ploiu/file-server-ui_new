package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.FolderApi
import java.io.File

class PreviewService(val folderClient: FolderClient, val fileClient: FileClient) {
    val cacheDir = File(System.getProperty("user.home") + "/.ploiu-file-server/cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }


    suspend fun getFolderPreview(folder: FolderApi): Map<Long, ByteArray> {
        val previews = folderClient.getPreviewsForFolder(folder.id)
    }

}
