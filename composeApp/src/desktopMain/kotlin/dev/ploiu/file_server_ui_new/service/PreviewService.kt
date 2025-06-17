package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onSuccess
import dev.ploiu.file_server_ui_new.ApiException
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.model.BatchFolderPreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.parseErrorFromResponse
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files


private data class CachedReadResult(
    val read: BatchFolderPreview,
    val missingFromDisk: Collection<Long>,
    val toDeleteFromDisk: Collection<File>
)

class PreviewService(
    private val folderService: FolderService,
    private val fileClient: FileClient,
    directoryService: DirectoryService
) {
    private val cacheDir = File(directoryService.getRootDirectory(), "/cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * creates a file handle that points to the preview cache dir for the passed folder. This does not create the folder itself.
     */
    fun folderCacheDir(folder: FolderApi) = File(cacheDir, folder.id.toString())

    @Throws(ApiException::class)
    suspend fun getFolderPreview(folder: FolderApi): Result<BatchFolderPreview, String> = coroutineScope {
        val folderCacheDir = folderCacheDir(folder)
        if (!folderCacheDir.exists()) {
            folderCacheDir.mkdirs()
        }
        val cached = readCachedPreviews(folder, folderCacheDir)
        launch(Dispatchers.IO) {
            for (toDelete in cached.toDeleteFromDisk) {
                toDelete.delete()
            }
        }
        /* if the number of files to pull is small (<= 20), we can pull them individually batched into even smaller groups.
         The number here is so small because the server is designed to run on a raspi, and we don't want to overwhelm it */
        if (cached.missingFromDisk.size <= 20) {
            val diskCache = cached.read.toMutableMap()
            val chunked = cached.missingFromDisk.chunked(5)
            for (chunk in chunked) {
                chunk.map {
                    async(Dispatchers.IO) {
                        val bytes = downloadPreview(it)
                        if (bytes != null) {
                            val previewLocation = File(folderCacheDir, "$it.png")
                            Files.write(previewLocation.toPath(), bytes)
                            Pair(it, bytes)
                        } else {
                            null
                        }
                    }
                }.awaitAll()
                    .filterNotNull()
                    .forEach { diskCache.put(it.first, it.second) }

            }
            Ok(diskCache)
        } else {
            folderCacheDir.deleteRecursively()
            folderCacheDir.mkdirs()
            folderService
                .getPreviewsForFolder(folder.id)
                .onSuccess { buildInitialCacheForFolder(folderCacheDir, it) }
        }
    }

    /**
     * Downloads the preview image for a given file ID. This does not save the preview image to disk.
     *
     * @param fileId The ID of the file to download the preview for.
     * @return The preview as a ByteArray, or null if no preview exists (HTTP 404).
     * @throws ApiException if the server returns an error other than 404.
     */
    @Throws(ApiException::class)
    suspend fun downloadPreview(fileId: Long): ByteArray? {
        val res = fileClient.getFilePreview(fileId)
        return if (res.isSuccessful) {
            res.body()!!.bytes()
        } else if (res.code() == 404) { // 404 means there's no preview, but it's not an error, so ignore it
            null
        } else {
            val error = parseErrorFromResponse(res)
            throw ApiException(error.message)
        }
    }

    /**
     * reads the preview directory for the folder and returns the state of the cached previews:
     * - read contains all the previews stored on the disk that can still be used (i.e. previews for files on the folder's metadata)
     * - missingFromDisk contains all the file IDs that need to have their previews downloaded from the server (i.e. previews on the folder metadata but not on the disk)
     * - toDeleteFromDisk contains all the file IDs that no longer exist in the folder, and therefore need to be cleaned up from the disk
     */
    private fun readCachedPreviews(folder: FolderApi, folderCache: File): CachedReadResult {
        val metadataFileIds = folder.files.map { it.id }
        if (!folderCache.exists()) {
            return CachedReadResult(emptyMap(), metadataFileIds, emptyList())
        }
        val apiFileIndex = folder.files.associateBy { it.id }
        val cachedFilesIndex = folderCache.listFiles().associateBy { it.name.replace(".png", "").toLong() }
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
    private fun buildInitialCacheForFolder(cacheDir: File, previews: Map<Long, ByteArray>) {
        for ((fileId, previewBytes) in previews) {
            val file = File(cacheDir, "$fileId.png")
            previewBytes.inputStream().use {
                Files.copy(it, file.toPath())
            }
        }
    }

}
