package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.*
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.PreviewClient
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.parseErrorFromResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.nio.file.Files


private data class CachedReadResult(
    val read: BatchFilePreview,
    val missingFromDisk: Collection<Long>,
    val toDeleteFromDisk: Collection<File>,
)

class DesktopPreviewService(
    private val fileClient: FileClient,
    private val previewClient: PreviewClient,
    directoryService: DirectoryService,
) : PreviewService {
    private val cacheDir = File(directoryService.getRootDirectory(), "/cache")
    private val log = KotlinLogging.logger { }

    companion object {
        /** the number of requests to make at a time for individual file previews. More previews should only be fetched once a chunk is done */
        const val FILE_PREVIEW_CHUNK_SIZE = 30
    }

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    override suspend fun getFolderPreview(folder: FolderApi): Flow<FilePreview> =
        coroutineScope {
            val folderCacheDir = folderCacheDir(folder.id)
            if (!folderCacheDir.exists()) {
                folderCacheDir.mkdirs()
            }
            val cached = readCachedPreviews(folder, folderCacheDir)
            launch(Dispatchers.IO) {
                for (toDelete in cached.toDeleteFromDisk) {
                    toDelete.delete()
                }
            }
            /* if the number of files to pull is small, we can pull them individually batched into even smaller groups.
            The number here is so small because the server is designed to run on a raspi, and we don't want to overwhelm it */
            if (cached.missingFromDisk.size <= 100) {
                val diskCache = cached.read.toMutableMap()
                val chunked = cached.missingFromDisk.chunked(FILE_PREVIEW_CHUNK_SIZE)
                for (chunk in chunked) {
                    chunk.map {
                        async(Dispatchers.IO) {
                            val downloaded = downloadPreview(it)
                            if (downloaded.isOk) {
                                val bytes = downloaded.unwrap()
                                if (bytes != null) {
                                    cachePreview(folderId = folder.id, fileId = it, previewBytes = bytes)
                                    it to bytes
                                } else {
                                    null
                                }
                            } else {
                                log.error { "Failed to download preview for file id $it: ${downloaded.unwrapError()}" }
                                null
                            }
                        }
                    }.awaitAll().filterNotNull().forEach { diskCache[it.first] = it.second }

                }
                flow {
                    for ((fileId, previewBytes) in diskCache) {
                        emit(fileId to previewBytes)
                    }
                }
            } else {
                folderCacheDir.deleteRecursively()
                folderCacheDir.mkdirs()
                // not only do we need to download the previews, but we also need to cache and re-emit them
                previewClient.downloadFolderPreviews(folder.id)
                    .flowOn(Dispatchers.IO)
                    .onEach {
                        cachePreview(folder.id, it.first, it.second)
                    }
            }
        }

    override suspend fun downloadPreview(fileId: Long): Result<ByteArray?, String> {
        val res = fileClient.getFilePreview(fileId)
        return if (res.isSuccessful) {
            Ok(res.body()!!.bytes())
        } else if (res.code() == 404) { // 404 means there's no preview, but it's not an error, so ignore it
            Ok(null)
        } else {
            Err(parseErrorFromResponse(res).message)
        }
    }

    override suspend fun getFilePreviews(vararg files: FileApi): Result<BatchFilePreview, String> =
        coroutineScope {
            // there could be any number of files in this list, so we need to be sure not to overwhelm the server with requests
            val chunks = files.toSet().chunked(FILE_PREVIEW_CHUNK_SIZE)
            val cached = mutableMapOf<Long, ByteArray>()
            for (chunk in chunks) {
                chunk.map { file ->
                    // by storing the file inside the parent folder's cache dir, we are helping build that folder's cache as well for next time it gets loaded
                    val previewDirectory = folderCacheDir(file.folderId)
                    async(Dispatchers.IO) {
                        previewDirectory.mkdirs()
                        val previewLocation = File(previewDirectory, "${file.id}.png")
                        if (previewLocation.exists()) {
                            Pair(file.id, previewLocation.readBytes())
                        } else {
                            val downloaded = downloadPreview(file.id)
                            if (downloaded.isOk) {
                                val bytes = downloaded.unwrap()
                                if (bytes != null) {
                                    Files.write(previewLocation.toPath(), bytes)
                                    Pair(file.id, bytes)
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull().forEach { cached[it.first] = it.second }
            }
            Ok(cached)
        }

    /**
     * creates a file handle that points to the preview cache dir for the passed folder. This does not create the folder itself.
     */
    private fun folderCacheDir(folderId: Long) = File(cacheDir, folderId.toString())

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
        val cachedFilesIndex =
            folderCache.listFiles().associateBy { it.name.replace(".png", "").toLong() }
        val missingFromDisk = apiFileIndex.keys - cachedFilesIndex.keys
        val toDeleteFromDisk =
            (cachedFilesIndex.keys - apiFileIndex.keys).map { cachedFilesIndex.getValue(it) }
        val fileCache = mutableMapOf<Long, ByteArray>()
        for ((id, file) in cachedFilesIndex) { // using readBytes here is fine because each preview is only around ~30-50kib
            fileCache[id] = file.readBytes()
        }
        return CachedReadResult(fileCache, missingFromDisk, toDeleteFromDisk)
    }

    private fun cachePreview(folderId: Long, fileId: Long, previewBytes: ByteArray) {
        val folderCacheDir = folderCacheDir(folderId)
        if (!folderCacheDir.exists()) {
            folderCacheDir.mkdirs()
        }
        val previewLocation = File(folderCacheDir, "$fileId.png")
        Files.write(previewLocation.toPath(), previewBytes)
    }

}
