package dev.ploiu.file_server_ui_new.service

import android.content.Context
import com.github.michaelbull.result.*
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.PreviewClient
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.util.parseErrorFromResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.file.Files

private data class CachedReadResult(
    val read: Flow<FilePreview>,
    val toDelete: Collection<File>,
    val missingFromDisk: Collection<Long>,
)

class AndroidPreviewService(
    private val fileClient: FileClient,
    private val previewClient: PreviewClient,
    private val context: Context,
) : PreviewService {
    private val log = KotlinLogging.logger { }

    companion object {
        /** the number of requests to make at a time for individual file previews. More previews should only be fetched once a chunk is done */
        const val FILE_PREVIEW_CHUNK_SIZE = 30
    }

    override suspend fun getFolderPreview(folder: FolderApi): Flow<FilePreview> = flow {
        val (cachedFlow, toDelete, toDownload) = readCachedPreviews(folder)
        withContext(Dispatchers.IO) {
            for (file in toDelete) {
                file.delete()
            }
        }
        emitAll(cachedFlow)
        // if we have a "small" number to pull individually, we can just call downloadPreview
        if (toDownload.size <= 100) {
            val chunked = toDownload.chunked(FILE_PREVIEW_CHUNK_SIZE)
            for (chunk in chunked) {
                val results = coroutineScope {
                    chunk.map {
                        async(Dispatchers.IO) {
                            val downloaded = downloadPreview(it)
                            if (downloaded.isOk) {
                                val bytes = downloaded.unwrap()
                                if (bytes != null) {
                                    cachePreview(fileId = it, previewBytes = bytes)
                                    it to bytes
                                } else {
                                    null
                                }
                            } else {
                                log.error { "Failed to download preview for file id $it: ${downloaded.unwrapError()}" }
                                null
                            }
                        }
                    }.awaitAll()
                }

                results.filterNotNull().forEach { emit(it) }
            }
        } else {
            // we're missing a _lot_ of previews, so download the entire folder now
            emitAll(
                previewClient.downloadFolderPreviews(folder.id).flowOn(Dispatchers.IO).onEach {
                    cachePreview(it.first, it.second)
                },
            )
        }
    }

    override suspend fun downloadPreview(fileId: Long): Result<ByteArray?, String> {
        val res = fileClient.getFilePreview(fileId)
        return if (res.isSuccessful) {
            Ok(res.body()!!.bytes())
        } else if (res.code() == 404) {
            // 404 means there's no preview, but it's not an error, so ignore it
            Ok(null)
        } else {
            Err(parseErrorFromResponse(res).message)
        }
    }

    override suspend fun getFilePreviews(vararg files: FileApi): Result<BatchFilePreview, String> = coroutineScope {
        // there could be any number of files in this list, so we need to be sure not to overwhelm the server with requests
        val chunks = files.toSet().chunked(FILE_PREVIEW_CHUNK_SIZE)
        val cached = mutableMapOf<Long, ByteArray>()
        for (chunk in chunks) {
            chunk.map { file ->
                // by storing the file inside the parent folder's cache dir, we are helping build that folder's cache as well for next time it gets loaded
                async(Dispatchers.IO) {
                    val previewLocation = File(context.cacheDir, "${file.id}.png")
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
     * reads the preview directory for the folder and returns the state of the cached previews:
     * - read contains all the previews stored on the disk that can still be used (i.e. previews for files on the folder's metadata)
     * - missingFromDisk contains all the file IDs that need to have their previews downloaded from the server (i.e. previews on the folder metadata but not on the disk)
     * - toDeleteFromDisk contains all the file IDs that no longer exist in the folder, and therefore need to be cleaned up from the disk
     */
    private fun readCachedPreviews(folder: FolderApi): CachedReadResult {
        val metadataFileIds = folder.files.map { it.id }.toSet()
        val cachedFiles = mutableMapOf<Long, File>()
        val uncachedFiles = mutableSetOf<Long>()
        for (id in metadataFileIds) {
            val f = File(context.cacheDir, "$id.png")
            if (f.exists()) {
                cachedFiles[id] = f
            } else {
                uncachedFiles += id
            }
        }

        // now that we know what we have, we need to know what needs to be removed
        val toDelete = (cachedFiles.keys - metadataFileIds).mapNotNull { cachedFiles[it] }

        return CachedReadResult(
            // unlike on desktop, android apps have much more limited memory allowances, so we need to keep as few in memory at a time as possible
            read = flow {
                for ((id, file) in cachedFiles) {
                    emit(id to file.readBytes())
                }
            },
            toDelete = toDelete,
            missingFromDisk = uncachedFiles,
        )
    }

    private fun cachePreview(fileId: Long, previewBytes: ByteArray) {
        val previewLocation = File(context.cacheDir, "$fileId.png")
        previewLocation.createNewFile()
        Files.write(previewLocation.toPath(), previewBytes)
    }
}
