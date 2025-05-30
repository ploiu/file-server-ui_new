package dev.ploiu.file_server_ui_new.model

data class FolderApi(
    val id: Long,
    val parentId: Long?,
    val path: String,
    val name: String,
    val folders: Collection<FolderApi>,
    val files: Collection<FileApi>,
    val tags: Collection<Tag>
)

data class CreateFolder (
    val name: String,
    val parentId: Long,
    val tags: Collection<Tag>
)

data class UpdateFolder (
    val id: Long,
    val name: String,
    val parentId: Long,
    val tags: Collection<Tag>
)
