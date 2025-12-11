package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable

@Serializable
data class FolderApi(
    val id: Long,
    val parentId: Long?,
    val path: String,
    val name: String,
    val folders: Collection<FolderApi>,
    val files: Collection<FileApi>,
    val tags: Collection<TaggedItemApi>,
) : FolderChild {
    fun toUpdateFolder() = UpdateFolder(
        id = id, name = name, parentId = parentId ?: 0, tags = tags,
    )

}

@Serializable
data class CreateFolder(
    val name: String,
    val parentId: Long,
    val tags: Collection<TaggedItemApi>,
)

@Serializable
data class UpdateFolder(
    val id: Long,
    val name: String,
    val parentId: Long,
    val tags: Collection<TaggedItemApi>,
)

/** keys are file ids, values are the associated preview */
typealias BatchFilePreview = Map<Long, ByteArray>

typealias FilePreview = Pair<Long, ByteArray>
