package dev.ploiu.file_server_ui_new.model

import java.io.File

data class FileApi(
    val id: Long,
    val folderId: Long,
    val name: String,
    val tags: Collection<Tag>,
    val size: Long,
    val dateCreated: String,
    val fileType: String
)

data class FileRequest (
    val id: Long,
    val folderId: Long,
    val name: String,
    val tags: Collection<Tag>
)

data class CreateFileRequest (
    val file: File,
    val extension: String,
    val folderId: Long?
)
