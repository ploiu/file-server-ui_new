package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable

@Serializable
data class FolderApi(
    val id: Long,
    val parentId: Long?,
    val path: String,
    val name: String,
    val folders: List<FolderApi>,
    val files: List<FileApi>,
    val tags: List<Tag>
)

@Serializable
data class CreateFolder(
    val name: String,
    val parentId: Long,
    val tags: Collection<Tag>
)

@Serializable
data class UpdateFolder(
    val id: Long,
    val name: String,
    val parentId: Long,
    val tags: Collection<Tag>
)

typealias BatchFolderPreview = Map<Long, ByteArray>
