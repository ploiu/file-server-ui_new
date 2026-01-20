package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Result
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.PreviewClient
import dev.ploiu.file_server_ui_new.model.BatchFilePreview
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FilePreview
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow

class AndroidPreviewService(private val fileClient: FileClient, private val previewClient: PreviewClient) :
    PreviewService {
    private val log = KotlinLogging.logger { }
    override suspend fun getFolderPreview(folder: FolderApi): Flow<FilePreview> {
        TODO("Not yet implemented")
    }

    override suspend fun downloadPreview(fileId: Long): Result<ByteArray?, String> {
        TODO("Not yet implemented")
    }

    override suspend fun getFilePreviews(vararg files: FileApi): Result<BatchFilePreview, String> {
        TODO("Not yet implemented")
    }
}
