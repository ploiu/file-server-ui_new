package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.FolderApi
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files


private data class CachedReadResult(
    val read: Map<Long, ByteArray>,
    val missingFromDisk: Collection<Long>,
    val toDeleteFromDisk: Collection<File>
)

class PreviewService(val folderClient: FolderClient, val fileClient: FileClient) {
    val cacheDir = File(System.getProperty("user.home") + "/.ploiu-file-server/cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    suspend fun getFolderPreview(folder: FolderApi): Map<Long, ByteArray> = coroutineScope {
        val cached = readCachedPreviews(folder)
        launch(Dispatchers.IO) {
            for (toDelete in cached.toDeleteFromDisk) {
                toDelete.delete()
            }
        }
        val diskCache = cached.read.toMutableMap()
        // if the number of files to pull is small (<= 20), we can pull them individually batched into even smaller groups. The number here is so small because the server is designed to run on a raspi
        if (cached.missingFromDisk.size <= 20) {
            val chunked = cached.missingFromDisk.chunked(5)
            for (chunk in chunked) {
                val previews = chunk.map { async(Dispatchers.IO) {
                    val previewResponse = fileClient.getFilePreview(it)
                    TODO("create a new method for getting a specific file preview that handles errors or otherwise adds the preview to the diskCache")
                } }.awaitAll()

            }
        } else {
            TODO("delete entire cache folder and download the whole thing again. Be sure to call buildInitialCacheForFolder")
        }
        TODO("pull cached previews and apply them to each FileEntry")
    }

    /**
     * reads the preview directory for the folder and returns the state of the cached previews:
     * - read contains all the previews stored on the disk that can still be used (i.e. previews for files on the folder's metadata)
     * - missingFromDisk contains all the file IDs that need to have their previews downloaded from the server (i.e. previews on the folder metadata but not on the disk)
     * - toDeleteFromDisk contains all the file IDs that no longer exist in the folder, and therefore need to be cleaned up from the disk
     */
    private fun readCachedPreviews(folder: FolderApi): CachedReadResult {
        val folderCache = File(cacheDir, folder.id.toString())
        val metadataFileIds = folder.files.map { it.id }
        if (!folderCache.exists()) {
            return CachedReadResult(emptyMap(), metadataFileIds, emptyList())
        }
        val apiFileIndex = folder.files.associateBy { it.id }
        val cachedFilesIndex = folderCache.listFiles().associateBy { it.name.toLong() }
        val missingFromDisk = apiFileIndex.keys - cachedFilesIndex.keys
        val toDeleteFromDisk = (cachedFilesIndex.keys - apiFileIndex.keys).map { cachedFilesIndex.getValue(it) }
        val fileCache = mutableMapOf<Long, ByteArray>()
        for ((id, file) in cachedFilesIndex) {
            // using readBytes here is fine because each preview is only around ~30-50kib
            fileCache.put(id, file.readBytes())
        }
        return CachedReadResult(fileCache, missingFromDisk, toDeleteFromDisk)
    }

    /**
     * Takes all the folder's previews and stores them in a folder-specific dir inside our cacheDir.
     *
     * This should only be run if no folder-specific cache dir exists yet. Each folder-specific directory is named after the folder id
     */
    private fun buildInitialCacheForFolder(folder: FolderApi, previews: Map<Long, ByteArray>) {
        val dir = File(cacheDir, folder.id.toString())
        dir.mkdirs()
        for ((fileId, previewBytes) in previews) {
            val file = File(dir, fileId.toString())
            file.createNewFile()
            previewBytes.inputStream().use {
                Files.copy(it, file.toPath())
            }
        }
    }

}
