package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FileApi(
    val id: Long,
    val folderId: Long = 0,
    val name: String,
    val tags: Collection<Tag>,
    val size: Long,
    val dateCreated: String,
    val fileType: String
)

@Serializable
data class FileRequest(
    val id: Long,
    val folderId: Long,
    val name: String,
    val tags: Collection<Tag>
)

data class CreateFileRequest(
    val folderId: Long,
    val file: File,
    val force: Boolean
)
