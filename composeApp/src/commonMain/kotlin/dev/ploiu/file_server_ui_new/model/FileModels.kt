package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FileApi(
    val id: Long,
    val folderId: Long = 0,
    val name: String,
    val tags: Collection<TaggedItemApi>,
    val size: Long,
    val dateCreated: String,
    val fileType: String,
): FolderChild {
    val extension: String
        get() {
            // fun fact, files can end with . on linux!
            return if (name.endsWith('.') || !name.contains('.')) {
                ""
            } else {
                name.substring(name.lastIndexOf('.') + 1)
            }
        }
    val nameWithoutExtension
        get() = if (!name.contains('.')) {
            name
        } else {
            name.substring(0, name.lastIndexOf('.'))
        }
    fun toFileRequest() = FileRequest(id = id, folderId = folderId, name = name, tags = tags)
}

fun FileApi.path(folder: FolderApi): String {
    val rootPart = "~/"
    return if (folder.name == "root") {
        rootPart + name
    } else {
        rootPart + folder.path + "/" + name
    }
}

@Serializable
data class FileRequest(
    val id: Long,
    val folderId: Long,
    val name: String,
    val tags: Collection<TaggedItemApi>,
)

data class CreateFileRequest(
    val folderId: Long,
    val file: File,
    val force: Boolean,
)
