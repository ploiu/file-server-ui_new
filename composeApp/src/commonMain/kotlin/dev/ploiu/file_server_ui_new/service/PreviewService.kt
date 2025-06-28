package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Result
import dev.ploiu.file_server_ui_new.ApiException
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi

interface PreviewService {
    /**
     * retrieves the previews of all files in the folder. This function utilizes a disk cache of the previews, and will strongly
     * prefer pulling from that cache when possible. This function will also clean up cached files that no longer exist in that folder.
     * If there are enough uncached files, it will instead prefer to download the entire folder cache instead.
     * TODO this can cause performance issues in folders that have a lot of files without previews, but files that do have previews and do get cached. Maybe.
     */
    suspend fun getFolderPreview(folder: FolderApi): Result<BatchFilePreview, String>

    /**
     * Downloads the preview image for a given file ID. This does not save the preview image to disk.
     *
     * @param fileId The ID of the file to download the preview for.
     * @return The preview as a ByteArray, or null if no preview exists (HTTP 404).
     * @throws ApiException if the server returns an error other than 404.
     */
    suspend fun downloadPreview(fileId: Long): Result<ByteArray?, String>

    /**
     * downloads the previews of the passed files. This function utilizes disk cache of the previews, and will strongly
     * prefer pulling from that cache when possible. Cached files are stored as if [getFolderPreview] was called; i.e. inside the folder's cache directory
     */
    suspend fun getFilePreviews(vararg files: FileApi): Result<BatchFilePreview, String>
}
