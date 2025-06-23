package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Result
import dev.ploiu.file_server_ui_new.model.BatchFolderPreview
import dev.ploiu.file_server_ui_new.model.FolderApi

interface PreviewService {
    suspend fun getFolderPreview(folder: FolderApi): Result<BatchFolderPreview, String>
    suspend fun downloadPreview(fileId: Long): Result<ByteArray?, String>
}
